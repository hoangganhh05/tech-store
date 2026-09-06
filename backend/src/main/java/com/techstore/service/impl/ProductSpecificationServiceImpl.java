package com.techstore.service.impl;

import com.techstore.dto.request.ProductSpecificationRequest;
import com.techstore.dto.response.ProductSpecificationResponse;
import com.techstore.entity.Product;
import com.techstore.entity.ProductSpecification;
import com.techstore.enums.ErrorCode;
import com.techstore.exception.BusinessException;
import com.techstore.repository.ProductRepository;
import com.techstore.repository.ProductSpecificationRepository;
import com.techstore.service.ProductSpecificationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ProductSpecificationServiceImpl implements ProductSpecificationService {

    private final ProductRepository productRepository;
    private final ProductSpecificationRepository productSpecificationRepository;

    public ProductSpecificationServiceImpl(
            ProductRepository productRepository,
            ProductSpecificationRepository productSpecificationRepository
    ) {
        this.productRepository = productRepository;
        this.productSpecificationRepository = productSpecificationRepository;
    }

    @Override
    public List<ProductSpecificationResponse> getSpecifications(Long productId) {
        findActiveProductOrThrow(productId);
        return productSpecificationRepository.findByProductIdOrderByDisplayOrderAscIdAsc(productId)
                .stream()
                .map(ProductSpecificationResponse::from)
                .toList();
    }

    @Override
    public ProductSpecificationResponse getSpecificationById(Long productId, Long specificationId) {
        findActiveProductOrThrow(productId);
        ProductSpecification spec = productSpecificationRepository.findByIdAndProductId(specificationId, productId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.PRODUCT_SPECIFICATION_NOT_FOUND,
                        "Không tìm thấy thông số kỹ thuật với ID: " + specificationId
                ));
        return ProductSpecificationResponse.from(spec);
    }

    @Override
    @Transactional
    public ProductSpecificationResponse createSpecification(Long productId, ProductSpecificationRequest request) {
        Product product = findActiveProductOrThrow(productId);

        String trimmedKey = request.specKey().trim();
        String trimmedValue = request.specValue().trim();

        if (productSpecificationRepository.existsByProductIdAndSpecKeyIgnoreCase(productId, trimmedKey)) {
            throw new BusinessException(
                    ErrorCode.SPECIFICATION_KEY_DUPLICATE,
                    String.format("Thông số kỹ thuật '%s' đã tồn tại cho sản phẩm này", trimmedKey)
            );
        }

        int displayOrder = request.displayOrder() != null ? request.displayOrder() : 0;
        ProductSpecification spec = new ProductSpecification(product, trimmedKey, trimmedValue, displayOrder);
        ProductSpecification saved = productSpecificationRepository.save(spec);

        return ProductSpecificationResponse.from(saved);
    }

    @Override
    @Transactional
    public ProductSpecificationResponse updateSpecification(Long productId, Long specificationId, ProductSpecificationRequest request) {
        findActiveProductOrThrow(productId);

        ProductSpecification spec = productSpecificationRepository.findByIdAndProductId(specificationId, productId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.PRODUCT_SPECIFICATION_NOT_FOUND,
                        "Không tìm thấy thông số kỹ thuật với ID: " + specificationId
                ));

        String trimmedKey = request.specKey().trim();
        String trimmedValue = request.specValue().trim();

        if (productSpecificationRepository.existsByProductIdAndSpecKeyIgnoreCaseAndIdNot(productId, trimmedKey, specificationId)) {
            throw new BusinessException(
                    ErrorCode.SPECIFICATION_KEY_DUPLICATE,
                    String.format("Thông số kỹ thuật '%s' đã tồn tại cho sản phẩm này", trimmedKey)
            );
        }

        spec.setSpecKey(trimmedKey);
        spec.setSpecValue(trimmedValue);
        if (request.displayOrder() != null) {
            spec.setDisplayOrder(request.displayOrder());
        }

        ProductSpecification saved = productSpecificationRepository.save(spec);
        return ProductSpecificationResponse.from(saved);
    }

    @Override
    @Transactional
    public void deleteSpecification(Long productId, Long specificationId) {
        findActiveProductOrThrow(productId);

        ProductSpecification spec = productSpecificationRepository.findByIdAndProductId(specificationId, productId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.PRODUCT_SPECIFICATION_NOT_FOUND,
                        "Không tìm thấy thông số kỹ thuật với ID: " + specificationId
                ));

        productSpecificationRepository.delete(spec);
    }

    private Product findActiveProductOrThrow(Long productId) {
        return productRepository.findByIdAndIsDeletedFalse(productId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.PRODUCT_NOT_FOUND,
                        "Không tìm thấy sản phẩm với ID: " + productId
                ));
    }
}
