package com.techstore.service.impl;

import com.techstore.dto.response.PageResponse;
import com.techstore.dto.response.StorefrontProductResponse;
import com.techstore.dto.response.WishlistItemResponse;
import com.techstore.entity.Product;
import com.techstore.entity.User;
import com.techstore.entity.Wishlist;
import com.techstore.enums.ErrorCode;
import com.techstore.enums.ProductStatus;
import com.techstore.enums.UserStatus;
import com.techstore.exception.BusinessException;
import com.techstore.repository.ProductRepository;
import com.techstore.repository.UserRepository;
import com.techstore.repository.WishlistRepository;
import com.techstore.service.StorefrontProductService;
import com.techstore.service.WishlistService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class WishlistServiceImpl implements WishlistService {

    private static final int MAX_PAGE_SIZE = 100;

    private final WishlistRepository wishlistRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final StorefrontProductService storefrontProductService;

    public WishlistServiceImpl(
            WishlistRepository wishlistRepository,
            UserRepository userRepository,
            ProductRepository productRepository,
            StorefrontProductService storefrontProductService
    ) {
        this.wishlistRepository = wishlistRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.storefrontProductService = storefrontProductService;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<StorefrontProductResponse> getWishlist(Long userId, int page, int size) {
        validatePage(page, size);
        findActiveUser(userId);

        Page<Product> products = wishlistRepository.findActiveProductsByUserId(
                userId,
                ProductStatus.ACTIVE,
                PageRequest.of(page, size)
        );
        java.util.Map<Long, StorefrontProductResponse> responseById = storefrontProductService
                .getProductsByIds(products.getContent().stream().map(Product::getId).toList())
                .stream()
                .collect(java.util.stream.Collectors.toMap(StorefrontProductResponse::id, response -> response));
        return new PageResponse<>(
                products.getContent().stream()
                        .map(product -> responseById.get(product.getId()))
                        .filter(java.util.Objects::nonNull)
                        .toList(),
                products.getNumber(),
                products.getSize(),
                products.getTotalElements(),
                products.getTotalPages(),
                products.isFirst(),
                products.isLast()
        );
    }

    @Override
    public WishlistItemResponse addToWishlist(Long userId, Long productId) {
        User user = findActiveUser(userId);
        Product product = findActiveProduct(productId);

        if (!wishlistRepository.existsByUserIdAndProductId(userId, productId)) {
            wishlistRepository.save(new Wishlist(user, product));
        }
        return new WishlistItemResponse(productId, true);
    }

    @Override
    public WishlistItemResponse removeFromWishlist(Long userId, Long productId) {
        findActiveUser(userId);
        if (productId == null || productId < 1) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Mã sản phẩm không hợp lệ");
        }
        wishlistRepository.deleteByUserIdAndProductId(userId, productId);
        return new WishlistItemResponse(productId, false);
    }

    private User findActiveUser(Long userId) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.INVALID_ACCESS_TOKEN, "Phiên đăng nhập không hợp lệ hoặc đã hết hạn");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.INVALID_ACCESS_TOKEN,
                        "Phiên đăng nhập không hợp lệ hoặc đã hết hạn"
                ));
        if (user.getStatus() == UserStatus.LOCKED) {
            throw new BusinessException(ErrorCode.ACCOUNT_LOCKED, "Tài khoản của bạn đã bị khóa. Vui lòng liên hệ hỗ trợ.");
        }
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.ACCOUNT_DISABLED, "Tài khoản hiện không thể sử dụng. Vui lòng liên hệ hỗ trợ.");
        }
        return user;
    }

    private Product findActiveProduct(Long productId) {
        if (productId == null || productId < 1) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Mã sản phẩm không hợp lệ");
        }
        Product product = productRepository.findByIdAndIsDeletedFalse(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND, "Không tìm thấy sản phẩm"));
        if (product.getStatus() != ProductStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND, "Sản phẩm đã ngừng kinh doanh");
        }
        return product;
    }

    private void validatePage(int page, int size) {
        if (page < 0) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Số trang không được âm");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "Kích thước trang phải từ 1 đến " + MAX_PAGE_SIZE);
        }
    }
}
