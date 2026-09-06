package com.techstore.service;

import com.techstore.dto.request.ProductCreateRequest;
import com.techstore.dto.request.ProductStatusUpdateRequest;
import com.techstore.dto.request.ProductUpdateRequest;
import com.techstore.dto.response.ProductResponse;

import java.util.List;

public interface ProductService {

    ProductResponse createProduct(ProductCreateRequest request);

    ProductResponse updateProduct(Long id, ProductUpdateRequest request);

    ProductResponse updateProductStatus(Long id, ProductStatusUpdateRequest request);

    List<ProductResponse> getAllAdminProducts();

    ProductResponse getProductById(Long id);
}
