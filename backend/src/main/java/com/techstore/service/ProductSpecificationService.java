package com.techstore.service;

import com.techstore.dto.request.ProductSpecificationRequest;
import com.techstore.dto.response.ProductSpecificationResponse;

import java.util.List;

public interface ProductSpecificationService {

    List<ProductSpecificationResponse> getSpecifications(Long productId);

    ProductSpecificationResponse getSpecificationById(Long productId, Long specificationId);

    ProductSpecificationResponse createSpecification(Long productId, ProductSpecificationRequest request);

    ProductSpecificationResponse updateSpecification(Long productId, Long specificationId, ProductSpecificationRequest request);

    void deleteSpecification(Long productId, Long specificationId);
}
