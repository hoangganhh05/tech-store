package com.techstore.service.impl;

import com.techstore.dto.request.ProductCreateRequest;
import com.techstore.dto.request.ProductStatusUpdateRequest;
import com.techstore.dto.request.ProductUpdateRequest;
import com.techstore.dto.response.ProductResponse;
import com.techstore.entity.Brand;
import com.techstore.entity.Category;
import com.techstore.entity.Product;
import com.techstore.entity.ProductVariant;
import com.techstore.enums.ErrorCode;
import com.techstore.enums.ProductStatus;
import com.techstore.exception.BusinessException;
import com.techstore.repository.BrandRepository;
import com.techstore.repository.CategoryRepository;
import com.techstore.repository.ProductRepository;
import com.techstore.repository.ProductVariantRepository;
import com.techstore.service.ProductService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

@Service
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final BrandRepository brandRepository;
    private final CategoryRepository categoryRepository;
    private final ProductVariantRepository productVariantRepository;
    private Predicate<Long> orderChecker = id -> false;

    public ProductServiceImpl(
            ProductRepository productRepository,
            BrandRepository brandRepository,
            CategoryRepository categoryRepository,
            ProductVariantRepository productVariantRepository
    ) {
        this.productRepository = productRepository;
        this.brandRepository = brandRepository;
        this.categoryRepository = categoryRepository;
        this.productVariantRepository = productVariantRepository;
    }

    public void setOrderChecker(Predicate<Long> orderChecker) {
        this.orderChecker = Objects.requireNonNull(orderChecker, "orderChecker must not be null");
    }

    @Override
    @Transactional
    public ProductResponse createProduct(ProductCreateRequest request) {
        String trimmedName = request.name().trim();

        Brand brand = brandRepository.findById(request.brandId())
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.BRAND_NOT_FOUND,
                        "Không tìm thấy thương hiệu với ID: " + request.brandId()
                ));

        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.CATEGORY_NOT_FOUND,
                        "Không tìm thấy danh mục với ID: " + request.categoryId()
                ));

        if (productRepository.existsByNameIgnoreCaseAndBrandIdAndIsDeletedFalse(trimmedName, request.brandId())) {
            throw new BusinessException(
                    ErrorCode.PRODUCT_NAME_DUPLICATE,
                    "Tên sản phẩm đã tồn tại trong cùng thương hiệu"
            );
        }

        ProductStatus targetStatus = request.status() != null ? request.status() : ProductStatus.DRAFT;
        if (targetStatus == ProductStatus.ACTIVE) {
            throw new BusinessException(
                    ErrorCode.PRODUCT_CANNOT_PUBLISH_WITHOUT_VARIANTS,
                    "Sản phẩm mới tạo phải ở trạng thái nháp (DRAFT), chỉ có thể chuyển sang đang bán khi có ít nhất một biến thể hợp lệ"
            );
        }

        Product product = new Product(
                trimmedName,
                request.description() != null ? request.description().trim() : null,
                brand,
                category,
                targetStatus
        );

        Product saved = productRepository.save(product);
        return ProductResponse.from(saved);
    }

    @Override
    @Transactional
    public ProductResponse updateProduct(Long id, ProductUpdateRequest request) {
        Product product = productRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.PRODUCT_NOT_FOUND,
                        "Không tìm thấy sản phẩm với ID: " + id
                ));

        String trimmedName = request.name().trim();

        Brand brand = brandRepository.findById(request.brandId())
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.BRAND_NOT_FOUND,
                        "Không tìm thấy thương hiệu với ID: " + request.brandId()
                ));

        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.CATEGORY_NOT_FOUND,
                        "Không tìm thấy danh mục với ID: " + request.categoryId()
                ));

        if (productRepository.existsByNameIgnoreCaseAndBrandIdAndIdNotAndIsDeletedFalse(trimmedName, request.brandId(), id)) {
            throw new BusinessException(
                    ErrorCode.PRODUCT_NAME_DUPLICATE,
                    "Tên sản phẩm đã tồn tại trong cùng thương hiệu"
            );
        }

        ProductStatus targetStatus = request.status() != null ? request.status() : product.getStatus();
        if (targetStatus == ProductStatus.ACTIVE && productVariantRepository.countByProductIdAndIsDeletedFalse(id) == 0) {
            throw new BusinessException(
                    ErrorCode.PRODUCT_CANNOT_PUBLISH_WITHOUT_VARIANTS,
                    "Sản phẩm chỉ có thể chuyển sang đang bán khi có ít nhất một biến thể hợp lệ"
            );
        }

        product.update(
                trimmedName,
                request.description() != null ? request.description().trim() : null,
                brand,
                category,
                targetStatus
        );

        return ProductResponse.from(product);
    }

    @Override
    @Transactional
    public ProductResponse updateProductStatus(Long id, ProductStatusUpdateRequest request) {
        Product product = productRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.PRODUCT_NOT_FOUND,
                        "Không tìm thấy sản phẩm với ID: " + id
                ));

        if (request.status() == ProductStatus.ACTIVE && productVariantRepository.countByProductIdAndIsDeletedFalse(id) == 0) {
            throw new BusinessException(
                    ErrorCode.PRODUCT_CANNOT_PUBLISH_WITHOUT_VARIANTS,
                    "Sản phẩm chỉ có thể chuyển sang đang bán khi có ít nhất một biến thể hợp lệ"
            );
        }

        product.setStatus(request.status());
        return ProductResponse.from(product);
    }

    @Override
    @Transactional
    public void deleteProduct(Long id) {
        Product product = productRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.PRODUCT_NOT_FOUND,
                        "Không tìm thấy sản phẩm với ID: " + id
                ));

        List<ProductVariant> variants = productVariantRepository.findByProductId(id);
        boolean hasOrders = orderChecker.test(id) || variants.stream().anyMatch(v -> orderChecker.test(v.getId()));
        if (hasOrders) {
            throw new BusinessException(
                    ErrorCode.PRODUCT_HAS_ORDERS,
                    "Không thể xoá sản phẩm đã phát sinh đơn hàng, vui lòng chuyển trạng thái sang ngừng bán"
            );
        }

        product.softDelete();
        for (ProductVariant variant : variants) {
            if (!variant.isDeleted()) {
                variant.softDelete();
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> getAllAdminProducts() {
        return productRepository.findAllByIsDeletedFalseOrderByCreatedAtDesc()
                .stream()
                .map(ProductResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getProductById(Long id) {
        Product product = productRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.PRODUCT_NOT_FOUND,
                        "Không tìm thấy sản phẩm với ID: " + id
                ));
        return ProductResponse.from(product);
    }
}
