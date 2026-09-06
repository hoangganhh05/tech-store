package com.techstore.service.impl;

import com.techstore.enums.ErrorCode;
import com.techstore.exception.BusinessException;
import com.techstore.service.FileStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class LocalFileStorageServiceImpl implements FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(LocalFileStorageServiceImpl.class);
    private static final long MAX_FILE_SIZE_BYTES = 5 * 1024 * 1024; // 5MB
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    private final Path uploadPath;

    public LocalFileStorageServiceImpl(@Value("${app.upload.dir:uploads/products}") String uploadDir) {
        this.uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.uploadPath);
        } catch (IOException e) {
            log.error("Could not create upload directory at {}", this.uploadPath, e);
            throw new BusinessException(ErrorCode.FILE_STORAGE_ERROR, "Không thể khởi tạo thư mục lưu trữ file");
        }
    }

    @Override
    public String storeProductImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_IMAGE_FORMAT, "File ảnh không được để trống");
        }

        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new BusinessException(ErrorCode.IMAGE_SIZE_EXCEEDED, "Dung lượng ảnh vượt quá giới hạn cho phép (tối đa 5MB)");
        }

        String originalFilename = file.getOriginalFilename();
        String extension = getFileExtension(originalFilename);
        String contentType = file.getContentType();

        boolean isValidExtension = extension != null && ALLOWED_EXTENSIONS.contains(extension.toLowerCase());
        boolean isValidContentType = contentType != null && ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase());

        if (!isValidExtension || !isValidContentType) {
            throw new BusinessException(ErrorCode.INVALID_IMAGE_FORMAT, "Định dạng file không hợp lệ. Chỉ chấp nhận các định dạng ảnh JPG, JPEG, PNG, WEBP");
        }

        String uniqueFilename = UUID.randomUUID() + "." + extension.toLowerCase();
        try {
            Path targetLocation = this.uploadPath.resolve(uniqueFilename).normalize();
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            return "/uploads/products/" + uniqueFilename;
        } catch (IOException e) {
            log.error("Failed to store file {}", uniqueFilename, e);
            throw new BusinessException(ErrorCode.FILE_STORAGE_ERROR, "Lưu file thất bại: " + e.getMessage());
        }
    }

    @Override
    public void deleteFile(String fileUrl) {
        if (fileUrl == null || fileUrl.isBlank()) {
            return;
        }

        try {
            String filename = fileUrl.substring(fileUrl.lastIndexOf('/') + 1);
            Path filePath = this.uploadPath.resolve(filename).normalize();
            if (Files.exists(filePath)) {
                Files.delete(filePath);
            }
        } catch (IOException e) {
            log.warn("Could not delete physical file for URL {}: {}", fileUrl, e.getMessage());
        }
    }

    private String getFileExtension(String filename) {
        if (filename == null || filename.lastIndexOf('.') == -1) {
            return null;
        }
        return filename.substring(filename.lastIndexOf('.') + 1);
    }
}
