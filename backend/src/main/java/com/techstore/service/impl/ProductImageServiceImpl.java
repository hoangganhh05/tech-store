package com.techstore.service.impl;

import com.techstore.dto.request.ProductImageUpdateRequest;
import com.techstore.dto.response.ProductImageResponse;
import com.techstore.entity.Product;
import com.techstore.entity.ProductImage;
import com.techstore.entity.ProductVariant;
import com.techstore.enums.ErrorCode;
import com.techstore.exception.BusinessException;
import com.techstore.repository.ProductImageRepository;
import com.techstore.repository.ProductRepository;
import com.techstore.repository.ProductVariantRepository;
import com.techstore.service.FileStorageService;
import com.techstore.service.ProductImageService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@Transactional
public class ProductImageServiceImpl implements ProductImageService {

    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ProductImageRepository productImageRepository;
    private final FileStorageService fileStorageService;

    public ProductImageServiceImpl(
            ProductRepository productRepository,
            ProductVariantRepository productVariantRepository,
            ProductImageRepository productImageRepository,
            FileStorageService fileStorageService
    ) {
        this.productRepository = productRepository;
        this.productVariantRepository = productVariantRepository;
        this.productImageRepository = productImageRepository;
        this.fileStorageService = fileStorageService;
    }

    @Override
    public ProductImageResponse uploadImage(Long productId, MultipartFile file, Long variantId, Boolean isPrimary) {
        Product product = getProductOrThrow(productId);

        ProductVariant variant = null;
        if (variantId != null) {
            variant = productVariantRepository.findByIdAndProductId(variantId, productId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_VARIANT_NOT_FOUND, "Không tìm thấy biến thể sản phẩm"));
        }

        String imageUrl = fileStorageService.storeProductImage(file);

        boolean hasPrimary = productImageRepository.existsByProductIdAndIsPrimaryTrue(productId);
        boolean effectivePrimary = Boolean.TRUE.equals(isPrimary) || !hasPrimary;

        if (effectivePrimary && hasPrimary) {
            productImageRepository.demotePrimaryForProduct(productId);
        }

        int displayOrder = (int) productImageRepository.countByProductId(productId);
        ProductImage image = new ProductImage(product, variant, imageUrl, effectivePrimary, displayOrder);
        ProductImage savedImage = productImageRepository.save(image);

        return ProductImageResponse.from(savedImage);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductImageResponse> getImagesByProductId(Long productId) {
        getProductOrThrow(productId);
        return productImageRepository.findByProductIdOrderByIsPrimaryDescDisplayOrderAscIdAsc(productId)
                .stream()
                .map(ProductImageResponse::from)
                .toList();
    }

    @Override
    public ProductImageResponse setPrimaryImage(Long productId, Long imageId) {
        getProductOrThrow(productId);

        ProductImage image = productImageRepository.findByIdAndProductId(imageId, productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_IMAGE_NOT_FOUND, "Không tìm thấy hình ảnh sản phẩm"));

        if (!Boolean.TRUE.equals(image.getIsPrimary())) {
            productImageRepository.demotePrimaryForProduct(productId);
            image.setIsPrimary(true);
            image = productImageRepository.save(image);
        }

        return ProductImageResponse.from(image);
    }

    @Override
    public ProductImageResponse updateImage(Long productId, Long imageId, ProductImageUpdateRequest request) {
        getProductOrThrow(productId);

        ProductImage image = productImageRepository.findByIdAndProductId(imageId, productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_IMAGE_NOT_FOUND, "Không tìm thấy hình ảnh sản phẩm"));

        if (request != null) {
            if (request.variantId() != null) {
                ProductVariant variant = productVariantRepository.findByIdAndProductId(request.variantId(), productId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_VARIANT_NOT_FOUND, "Không tìm thấy biến thể sản phẩm"));
                image.setVariant(variant);
            } else {
                image.setVariant(null);
            }

            if (request.displayOrder() != null) {
                image.setDisplayOrder(request.displayOrder());
            }

            image = productImageRepository.save(image);
        }

        return ProductImageResponse.from(image);
    }

    @Override
    public void deleteImage(Long productId, Long imageId) {
        getProductOrThrow(productId);

        ProductImage image = productImageRepository.findByIdAndProductId(imageId, productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_IMAGE_NOT_FOUND, "Không tìm thấy hình ảnh sản phẩm"));

        boolean wasPrimary = Boolean.TRUE.equals(image.getIsPrimary());
        String fileUrl = image.getImageUrl();

        productImageRepository.delete(image);
        productImageRepository.flush();

        if (wasPrimary) {
            List<ProductImage> remaining = productImageRepository.findByProductIdOrderByIsPrimaryDescDisplayOrderAscIdAsc(productId);
            if (!remaining.isEmpty()) {
                ProductImage newPrimary = remaining.get(0);
                newPrimary.setIsPrimary(true);
                productImageRepository.save(newPrimary);
            }
        }

        fileStorageService.deleteFile(fileUrl);
    }

    private Product getProductOrThrow(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND, "Không tìm thấy sản phẩm"));
    }
}
