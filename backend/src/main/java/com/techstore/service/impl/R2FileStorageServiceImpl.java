package com.techstore.service.impl;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.techstore.config.R2StorageProperties;
import com.techstore.enums.ErrorCode;
import com.techstore.exception.BusinessException;
import com.techstore.service.FileStorageService;

import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/** Stores product images in Cloudflare R2 through its S3-compatible API. */
@Service
@Profile("prod")
public class R2FileStorageServiceImpl implements FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(R2FileStorageServiceImpl.class);
    private static final long MAX_FILE_SIZE_BYTES = 5 * 1024 * 1024;
    private static final String OBJECT_PREFIX = "products/";
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

    private final S3Client s3Client;
    private final String bucket;
    private final String publicBaseUrl;

    public R2FileStorageServiceImpl(S3Client s3Client, R2StorageProperties properties) {
        this.s3Client = s3Client;
        this.bucket = properties.bucket();
        this.publicBaseUrl = trimTrailingSlashes(properties.publicBaseUrl());
    }

    @Override
    public String storeProductImage(MultipartFile file) {
        validate(file);

        String extension = getFileExtension(file.getOriginalFilename());
        String key = OBJECT_PREFIX + UUID.randomUUID() + "." + extension.toLowerCase(Locale.ROOT);
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(file.getContentType())
                .contentLength(file.getSize())
                .cacheControl("public, max-age=31536000, immutable")
                .build();

        try (InputStream inputStream = file.getInputStream()) {
            s3Client.putObject(request, RequestBody.fromInputStream(inputStream, file.getSize()));
            return publicBaseUrl + "/" + key;
        } catch (IOException | SdkException exception) {
            log.error("Could not store product image in R2: {}", key, exception);
            throw new BusinessException(ErrorCode.FILE_STORAGE_ERROR, "Lưu file thất bại");
        }
    }

    @Override
    public void deleteFile(String fileUrl) {
        String key = extractR2Key(fileUrl);
        if (key == null) {
            // Existing local URLs can remain in the database during migration;
            // they are not objects in R2 and must not be sent for deletion.
            return;
        }

        try {
            s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
        } catch (SdkException exception) {
            // Database deletion has already succeeded. Log the orphan clean-up
            // failure without breaking the admin operation.
            log.warn("Could not delete product image from R2: {}", key, exception);
        }
    }

    private static void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_IMAGE_FORMAT, "File ảnh không được để trống");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new BusinessException(ErrorCode.IMAGE_SIZE_EXCEEDED,
                    "Dung lượng ảnh vượt quá giới hạn cho phép (tối đa 5MB)");
        }

        String extension = getFileExtension(file.getOriginalFilename());
        String contentType = file.getContentType();
        boolean validExtension = extension != null && ALLOWED_EXTENSIONS.contains(extension.toLowerCase(Locale.ROOT));
        boolean validContentType = contentType != null
                && ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT));
        if (!validExtension || !validContentType) {
            throw new BusinessException(ErrorCode.INVALID_IMAGE_FORMAT,
                    "Định dạng file không hợp lệ. Chỉ chấp nhận các định dạng ảnh JPG, JPEG, PNG, WEBP");
        }
    }

    private String extractR2Key(String fileUrl) {
        if (fileUrl == null || fileUrl.isBlank()) {
            return null;
        }

        try {
            URI base = URI.create(publicBaseUrl);
            URI candidate = URI.create(fileUrl);
            if (!base.getScheme().equalsIgnoreCase(candidate.getScheme())
                    || !base.getHost().equalsIgnoreCase(candidate.getHost())) {
                return null;
            }

            String basePath = trimTrailingSlashes(base.getPath());
            String candidatePath = candidate.getPath();
            String prefix = (basePath == null || basePath.isBlank() ? "" : basePath) + "/";
            if (!candidatePath.startsWith(prefix)) {
                return null;
            }

            String key = candidatePath.substring(prefix.length());
            return key.startsWith(OBJECT_PREFIX) && !key.contains("..") ? key : null;
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private static String getFileExtension(String filename) {
        if (filename == null) {
            return null;
        }
        int dot = filename.lastIndexOf('.');
        return dot >= 0 && dot < filename.length() - 1 ? filename.substring(dot + 1) : null;
    }

    private static String trimTrailingSlashes(String value) {
        if (value == null) {
            return "";
        }
        return value.replaceAll("/+$", "");
    }
}
