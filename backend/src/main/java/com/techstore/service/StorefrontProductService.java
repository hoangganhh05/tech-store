package com.techstore.service;

import com.techstore.dto.response.CategoryResponse;
import com.techstore.dto.response.StorefrontHomeResponse;
import com.techstore.dto.response.StorefrontProductResponse;

import java.util.List;

public interface StorefrontProductService {

    StorefrontHomeResponse getHomeData(int limit);

    List<StorefrontProductResponse> getFeaturedProducts(int limit);

    List<StorefrontProductResponse> getNewArrivals(int limit);

    List<StorefrontProductResponse> getOnSaleProducts(int limit);

    List<CategoryResponse> getFeaturedCategories();

    List<StorefrontProductResponse> getProducts(Long categoryId);
}

