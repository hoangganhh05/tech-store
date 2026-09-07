package com.techstore.service;

import com.techstore.dto.response.BrandResponse;
import com.techstore.dto.response.CategoryResponse;
import com.techstore.dto.response.PageResponse;
import com.techstore.dto.response.StorefrontHomeResponse;
import com.techstore.dto.response.StorefrontProductDetailResponse;
import com.techstore.dto.response.StorefrontProductResponse;
import com.techstore.dto.response.VariantStockResponse;

import java.math.BigDecimal;
import java.util.List;

public interface StorefrontProductService {

    StorefrontHomeResponse getHomeData(int limit);

    List<StorefrontProductResponse> getFeaturedProducts(int limit);

    List<StorefrontProductResponse> getNewArrivals(int limit);

    List<StorefrontProductResponse> getOnSaleProducts(int limit);

    List<CategoryResponse> getFeaturedCategories();

    List<StorefrontProductResponse> getProducts(Long categoryId);

    List<StorefrontProductResponse> getProducts(Long categoryId, List<Long> brandIds, BigDecimal priceMin, BigDecimal priceMax);

    List<StorefrontProductResponse> getProducts(Long categoryId, List<Long> brandIds, BigDecimal priceMin, BigDecimal priceMax, String sortBy, String sortDir);

    PageResponse<StorefrontProductResponse> getPaginatedProducts(
            Long categoryId,
            List<Long> brandIds,
            BigDecimal priceMin,
            BigDecimal priceMax,
            String sortBy,
            String sortDir,
            int page,
            int size
    );

    List<StorefrontProductResponse> searchProducts(String query);

    List<BrandResponse> getFeaturedBrands();

    StorefrontProductDetailResponse getProductDetail(Long id);

    VariantStockResponse getVariantStock(Long productId, Long variantId);
}

