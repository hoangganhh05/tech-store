package com.techstore.service;

import com.techstore.dto.request.ProductCreateRequest;
import com.techstore.dto.response.ProductResponse;

import java.util.List;

public interface ProductService {

    ProductResponse createProduct(ProductCreateRequest request);

    List<ProductResponse> getAllAdminProducts();

    ProductResponse getProductById(Long id);
}

