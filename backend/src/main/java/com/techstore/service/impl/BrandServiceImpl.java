package com.techstore.service.impl;

import com.techstore.dto.request.BrandRequest;
import com.techstore.dto.response.BrandResponse;
import com.techstore.entity.Brand;
import com.techstore.enums.ErrorCode;
import com.techstore.exception.BusinessException;
import com.techstore.repository.BrandRepository;
import com.techstore.repository.ProductRepository;
import com.techstore.security.SafeUrlValidator;
import com.techstore.service.BrandService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class BrandServiceImpl implements BrandService {

    private final BrandRepository brandRepository;
    private final ProductRepository productRepository;

    public BrandServiceImpl(BrandRepository brandRepository, ProductRepository productRepository) {
        this.brandRepository = brandRepository;
        this.productRepository = productRepository;
    }

    @Override
    @Transactional
    public BrandResponse createBrand(BrandRequest request) {
        if (brandRepository.existsByNameIgnoreCase(request.name())) {
            throw new BusinessException(ErrorCode.BRAND_NAME_DUPLICATE, "Tên thương hiệu đã tồn tại trong hệ thống");
        }

        Brand brand = new Brand(
                request.name(),
                SafeUrlValidator.normalizeImageUrl(request.logoUrl()),
                request.description()
        );

        Brand savedBrand = brandRepository.save(brand);
        return BrandResponse.from(savedBrand);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BrandResponse> getAllBrands() {
        return brandRepository.findAllByOrderByNameAsc()
                .stream()
                .map(BrandResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public BrandResponse getBrandById(Long id) {
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.BRAND_NOT_FOUND, "Không tìm thấy thương hiệu với ID: " + id));
        return BrandResponse.from(brand);
    }

    @Override
    @Transactional
    public BrandResponse updateBrand(Long id, BrandRequest request) {
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.BRAND_NOT_FOUND, "Không tìm thấy thương hiệu với ID: " + id));

        if (brandRepository.existsByNameIgnoreCaseAndIdNot(request.name(), id)) {
            throw new BusinessException(ErrorCode.BRAND_NAME_DUPLICATE, "Tên thương hiệu đã tồn tại trong hệ thống");
        }

        brand.update(
                request.name(),
                SafeUrlValidator.normalizeImageUrl(request.logoUrl()),
                request.description()
        );

        Brand updatedBrand = brandRepository.save(brand);
        return BrandResponse.from(updatedBrand);
    }

    @Override
    @Transactional
    public void deleteBrand(Long id) {
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.BRAND_NOT_FOUND, "Không tìm thấy thương hiệu với ID: " + id));

        if (hasAssociatedProducts(id)) {
            throw new BusinessException(ErrorCode.BRAND_HAS_PRODUCTS, "Không thể xoá thương hiệu đang gắn với sản phẩm");
        }

        brandRepository.delete(brand);
    }

    private boolean hasAssociatedProducts(Long brandId) {
        return productRepository.existsByBrandId(brandId);
    }
}
