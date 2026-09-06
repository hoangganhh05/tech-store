package com.techstore.service;

import com.techstore.dto.request.ProductVariantRequest;
import com.techstore.dto.response.ProductVariantResponse;

import java.util.List;

public interface ProductVariantService {

    ProductVariantResponse createVariant(Long productId, ProductVariantRequest request);

    List<ProductVariantResponse> getVariantsByProductId(Long productId);

    ProductVariantResponse getVariantById(Long productId, Long variantId);

    ProductVariantResponse updateVariant(Long productId, Long variantId, ProductVariantRequest request);

    void deleteVariant(Long productId, Long variantId);
}
