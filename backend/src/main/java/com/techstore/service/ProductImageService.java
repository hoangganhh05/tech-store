package com.techstore.service;

import com.techstore.dto.request.ProductImageUpdateRequest;
import com.techstore.dto.response.ProductImageResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ProductImageService {

    ProductImageResponse uploadImage(Long productId, MultipartFile file, Long variantId, Boolean isPrimary);

    List<ProductImageResponse> getImagesByProductId(Long productId);

    ProductImageResponse setPrimaryImage(Long productId, Long imageId);

    ProductImageResponse updateImage(Long productId, Long imageId, ProductImageUpdateRequest request);

    void deleteImage(Long productId, Long imageId);
}
