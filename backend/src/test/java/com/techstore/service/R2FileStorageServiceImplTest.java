package com.techstore.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import com.techstore.config.R2StorageProperties;
import com.techstore.enums.ErrorCode;
import com.techstore.exception.BusinessException;
import com.techstore.service.impl.R2FileStorageServiceImpl;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@ExtendWith(MockitoExtension.class)
class R2FileStorageServiceImplTest {

    @Mock
    private S3Client s3Client;

    private R2FileStorageServiceImpl storageService;

    @BeforeEach
    void setUp() {
        storageService = new R2FileStorageServiceImpl(
                s3Client,
                new R2StorageProperties(
                        "https://account.r2.cloudflarestorage.com",
                        "auto",
                        "techstore-products",
                        "access-key",
                        "secret-key",
                        "https://pub-example.r2.dev"
                )
        );
    }

    @Test
    void storesImageWithStablePublicUrlAndCacheMetadata() throws Exception {
        MultipartFile file = new MockMultipartFile(
                "file",
                "phone.PNG",
                "image/png",
                "image bytes".getBytes(StandardCharsets.UTF_8)
        );

        String publicUrl = storageService.storeProductImage(file);

        org.junit.jupiter.api.Assertions.assertTrue(publicUrl.startsWith("https://pub-example.r2.dev/products/"));
        org.junit.jupiter.api.Assertions.assertTrue(publicUrl.endsWith(".png"));

        ArgumentCaptor<PutObjectRequest> requestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(requestCaptor.capture(), any(RequestBody.class));
        PutObjectRequest request = requestCaptor.getValue();
        assertEquals("techstore-products", request.bucket());
        assertEquals("image/png", request.contentType());
        assertEquals("public, max-age=31536000, immutable", request.cacheControl());
    }

    @Test
    void rejectsUnsupportedImageBeforeCallingR2() {
        MultipartFile file = new MockMultipartFile(
                "file",
                "document.pdf",
                "application/pdf",
                "not an image".getBytes(StandardCharsets.UTF_8)
        );

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> storageService.storeProductImage(file)
        );

        assertEquals(ErrorCode.INVALID_IMAGE_FORMAT, exception.getErrorCode());
        verifyNoInteractions(s3Client);
    }

    @Test
    void deletesOnlyObjectsBelongingToConfiguredPublicBucket() {
        storageService.deleteFile("https://pub-example.r2.dev/products/abc.jpg");

        ArgumentCaptor<DeleteObjectRequest> requestCaptor = ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(s3Client).deleteObject(requestCaptor.capture());
        assertEquals("techstore-products", requestCaptor.getValue().bucket());
        assertEquals("products/abc.jpg", requestCaptor.getValue().key());

        storageService.deleteFile("https://another-host.example/products/abc.jpg");
        storageService.deleteFile("/uploads/products/abc.jpg");
    }

    @Test
    void acceptsMultipartFileInputStreamAndPreservesSize() throws Exception {
        byte[] content = "webp bytes".getBytes(StandardCharsets.UTF_8);
        MultipartFile file = new MockMultipartFile(
                "file",
                "phone.webp",
                "image/webp",
                new ByteArrayInputStream(content)
        );

        storageService.storeProductImage(file);

        ArgumentCaptor<PutObjectRequest> requestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(requestCaptor.capture(), any(RequestBody.class));
        assertEquals(content.length, requestCaptor.getValue().contentLength());
    }
}
