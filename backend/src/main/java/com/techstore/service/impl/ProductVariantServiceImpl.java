package com.techstore.service.impl;

import com.techstore.dto.request.ProductVariantRequest;
import com.techstore.dto.response.ProductVariantResponse;
import com.techstore.entity.Inventory;
import com.techstore.entity.Product;
import com.techstore.entity.ProductVariant;
import com.techstore.enums.ErrorCode;
import com.techstore.enums.VariantStatus;
import com.techstore.exception.BusinessException;
import com.techstore.repository.InventoryRepository;
import com.techstore.repository.ProductRepository;
import com.techstore.repository.ProductVariantRepository;
import com.techstore.service.ProductVariantService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

@Service
public class ProductVariantServiceImpl implements ProductVariantService {

    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final InventoryRepository inventoryRepository;
    private Predicate<Long> orderChecker = id -> false;

    public ProductVariantServiceImpl(
            ProductRepository productRepository,
            ProductVariantRepository productVariantRepository,
            InventoryRepository inventoryRepository
    ) {
        this.productRepository = productRepository;
        this.productVariantRepository = productVariantRepository;
        this.inventoryRepository = inventoryRepository;
    }

    public void setOrderChecker(Predicate<Long> orderChecker) {
        this.orderChecker = Objects.requireNonNull(orderChecker, "orderChecker must not be null");
    }

    @Override
    @Transactional
    public ProductVariantResponse createVariant(Long productId, ProductVariantRequest request) {
        Product product = findProductOrThrow(productId);
        validatePriceAndOriginalPrice(request.price(), request.originalPrice());

        String trimmedSku = request.sku().trim().toUpperCase();
        if (productVariantRepository.existsBySkuIgnoreCase(trimmedSku)) {
            throw new BusinessException(
                    ErrorCode.VARIANT_SKU_DUPLICATE,
                    "Mã SKU đã tồn tại trong hệ thống: " + trimmedSku
            );
        }

        String color = request.color() != null ? request.color().trim() : null;
        String storage = request.storage() != null ? request.storage().trim() : null;
        VariantStatus status = request.status() != null ? request.status() : VariantStatus.ACTIVE;

        ProductVariant variant = new ProductVariant(
                product,
                trimmedSku,
                color,
                storage,
                request.price(),
                request.originalPrice(),
                request.stockQuantity(),
                status
        );

        ProductVariant saved = productVariantRepository.save(variant);
        int initialStock = request.stockQuantity() != null ? Math.max(0, request.stockQuantity()) : 0;
        Inventory inventory = new Inventory(saved, initialStock, 0, 5);
        inventoryRepository.save(inventory);

        return ProductVariantResponse.from(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductVariantResponse> getVariantsByProductId(Long productId) {
        findProductOrThrow(productId);
        return productVariantRepository.findByProductIdAndIsDeletedFalseOrderByCreatedAtAsc(productId)
                .stream()
                .map(ProductVariantResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ProductVariantResponse getVariantById(Long productId, Long variantId) {
        findProductOrThrow(productId);
        ProductVariant variant = findVariantOrThrow(productId, variantId);
        return ProductVariantResponse.from(variant);
    }

    @Override
    @Transactional
    public ProductVariantResponse updateVariant(Long productId, Long variantId, ProductVariantRequest request) {
        findProductOrThrow(productId);
        ProductVariant variant = findVariantOrThrow(productId, variantId);
        validatePriceAndOriginalPrice(request.price(), request.originalPrice());

        String trimmedSku = request.sku().trim().toUpperCase();
        if (productVariantRepository.existsBySkuIgnoreCaseAndIdNot(trimmedSku, variantId)) {
            throw new BusinessException(
                    ErrorCode.VARIANT_SKU_DUPLICATE,
                    "Mã SKU đã tồn tại trong hệ thống: " + trimmedSku
            );
        }

        String color = request.color() != null ? request.color().trim() : null;
        String storage = request.storage() != null ? request.storage().trim() : null;

        variant.update(
                trimmedSku,
                color,
                storage,
                request.price(),
                request.originalPrice(),
                request.stockQuantity(),
                request.status()
        );

        return ProductVariantResponse.from(variant);
    }

    @Override
    @Transactional
    public void deleteVariant(Long productId, Long variantId) {
        findProductOrThrow(productId);
        ProductVariant variant = findVariantOrThrow(productId, variantId);

        if (orderChecker.test(variantId)) {
            throw new BusinessException(
                    ErrorCode.VARIANT_HAS_ORDERS,
                    "Không thể xoá biến thể đã phát sinh đơn hàng, vui lòng chuyển trạng thái sang ngừng bán"
            );
        }

        variant.softDelete();
    }

    private Product findProductOrThrow(Long productId) {
        return productRepository.findByIdAndIsDeletedFalse(productId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.PRODUCT_NOT_FOUND,
                        "Không tìm thấy sản phẩm với ID: " + productId
                ));
    }

    private ProductVariant findVariantOrThrow(Long productId, Long variantId) {
        return productVariantRepository.findByIdAndProductIdAndIsDeletedFalse(variantId, productId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.PRODUCT_VARIANT_NOT_FOUND,
                        "Không tìm thấy biến thể với ID: " + variantId
                ));
    }

    private void validatePriceAndOriginalPrice(BigDecimal price, BigDecimal originalPrice) {
        if (price == null || price.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(
                    ErrorCode.VARIANT_INVALID_PRICE,
                    "Giá bán phải lớn hơn hoặc bằng 0"
            );
        }

        if (originalPrice != null) {
            if (originalPrice.compareTo(BigDecimal.ZERO) < 0) {
                throw new BusinessException(
                    ErrorCode.VARIANT_INVALID_PRICE,
                    "Giá gốc phải lớn hơn hoặc bằng 0"
                );
            }
            if (originalPrice.compareTo(price) < 0) {
                throw new BusinessException(
                        ErrorCode.VARIANT_INVALID_PRICE,
                        "Giá gốc phải lớn hơn hoặc bằng giá bán"
                );
            }
        }
    }
}
