package com.techstore.service.impl;

import com.techstore.dto.request.AddToCartRequest;
import com.techstore.dto.request.UpdateCartItemRequest;
import com.techstore.dto.response.CartItemResponse;
import com.techstore.dto.response.CartItemStockIssueResponse;
import com.techstore.dto.response.CartResponse;
import com.techstore.dto.response.CartSyncResponse;
import com.techstore.dto.response.CartValidationResponse;
import com.techstore.entity.Cart;
import com.techstore.entity.CartItem;
import com.techstore.entity.Inventory;
import com.techstore.entity.Product;
import com.techstore.entity.ProductImage;
import com.techstore.entity.ProductVariant;
import com.techstore.entity.User;
import com.techstore.enums.ErrorCode;
import com.techstore.enums.ProductStatus;
import com.techstore.enums.StockIssueType;
import com.techstore.exception.BusinessException;
import com.techstore.repository.CartItemRepository;
import com.techstore.repository.CartRepository;
import com.techstore.repository.InventoryRepository;
import com.techstore.repository.ProductImageRepository;
import com.techstore.repository.ProductVariantRepository;
import com.techstore.repository.UserRepository;
import com.techstore.service.CartService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
public class CartServiceImpl implements CartService {

    public static final BigDecimal DEFAULT_SHIPPING_FEE = new BigDecimal("30000");
    public static final BigDecimal FREE_SHIPPING_THRESHOLD = new BigDecimal("5000000");

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;
    private final ProductVariantRepository productVariantRepository;
    private final InventoryRepository inventoryRepository;
    private final ProductImageRepository productImageRepository;

    public CartServiceImpl(
            CartRepository cartRepository,
            CartItemRepository cartItemRepository,
            UserRepository userRepository,
            ProductVariantRepository productVariantRepository,
            InventoryRepository inventoryRepository,
            ProductImageRepository productImageRepository
    ) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.userRepository = userRepository;
        this.productVariantRepository = productVariantRepository;
        this.inventoryRepository = inventoryRepository;
        this.productImageRepository = productImageRepository;
    }

    @Override
    @Transactional
    public CartResponse addToCart(Long userId, String sessionId, AddToCartRequest request) {
        if (request == null || request.getVariantId() == null || request.getVariantId() <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Mã biến thể không hợp lệ");
        }
        int quantityToAdd = request.getQuantity() != null ? request.getQuantity() : 1;
        if (quantityToAdd <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Số lượng phải lớn hơn 0");
        }

        // 1. Tìm hoặc tạo giỏ hàng
        Cart cart = getOrCreateCart(userId, sessionId);

        // 2. Kiểm tra biến thể sản phẩm
        ProductVariant variant = productVariantRepository.findById(request.getVariantId())
                .filter(v -> !v.isDeleted())
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_VARIANT_NOT_FOUND, "Biến thể sản phẩm không tồn tại hoặc đã bị xoá"));

        Product product = variant.getProduct();
        if (product == null || product.isDeleted() || product.getStatus() != ProductStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND, "Sản phẩm không tồn tại hoặc đã ngừng kinh doanh");
        }

        // 3. Kiểm tra tồn kho khả dụng
        int availableStock = getAvailableStock(variant);
        if (availableStock <= 0) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_STOCK, "Sản phẩm đã hết hàng");
        }

        // 4. Kiểm tra dòng sản phẩm đã có trong giỏ chưa
        Optional<CartItem> existingItemOpt = cartItemRepository.findByCartIdAndVariantId(cart.getId(), variant.getId());
        int currentQuantityInCart = existingItemOpt.map(CartItem::getQuantity).orElse(0);
        int targetQuantity = currentQuantityInCart + quantityToAdd;

        if (targetQuantity > availableStock) {
            int canAddMore = Math.max(0, availableStock - currentQuantityInCart);
            throw new BusinessException(
                    ErrorCode.INSUFFICIENT_STOCK,
                    String.format("Số lượng vượt quá tồn kho khả dụng. Kho chỉ còn %d sản phẩm (trong giỏ đã có %d, có thể thêm tối đa %d).",
                            availableStock, currentQuantityInCart, canAddMore)
            );
        }

        // 5. Cập nhật hoặc tạo mới CartItem
        if (existingItemOpt.isPresent()) {
            CartItem item = existingItemOpt.get();
            item.setQuantity(targetQuantity);
            cartItemRepository.save(item);
        } else {
            CartItem newItem = new CartItem(cart, variant, quantityToAdd);
            cartItemRepository.save(newItem);
            cart.getItems().add(newItem);
        }

        return mapToCartResponse(cart);
    }

    @Override
    @Transactional(readOnly = true)
    public CartResponse getCart(Long userId, String sessionId) {
        Optional<Cart> cartOpt = findCart(userId, sessionId);
        if (cartOpt.isEmpty()) {
            return new CartResponse(null, 0, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, false, false, List.of());
        }
        return mapToCartResponse(cartOpt.get());
    }

    @Override
    @Transactional
    public CartResponse updateCartItemQuantity(Long userId, String sessionId, Long itemId, UpdateCartItemRequest request) {
        if (itemId == null || itemId <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Mã dòng sản phẩm giỏ hàng không hợp lệ");
        }
        if (request == null || request.getQuantity() == null || request.getQuantity() <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Số lượng phải lớn hơn 0");
        }

        Cart cart = findCart(userId, sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CART_NOT_FOUND, "Giỏ hàng không tồn tại"));

        CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CART_ITEM_NOT_FOUND, "Sản phẩm không có trong giỏ hàng"));

        if (!Objects.equals(item.getCart().getId(), cart.getId())) {
            throw new BusinessException(ErrorCode.CART_ITEM_NOT_FOUND, "Sản phẩm không thuộc giỏ hàng hiện tại");
        }

        ProductVariant variant = item.getVariant();
        if (variant == null || variant.isDeleted()) {
            throw new BusinessException(ErrorCode.PRODUCT_VARIANT_NOT_FOUND, "Biến thể sản phẩm không tồn tại hoặc đã bị xoá");
        }

        Product product = variant.getProduct();
        if (product == null || product.isDeleted() || product.getStatus() != ProductStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND, "Sản phẩm không tồn tại hoặc đã ngừng kinh doanh");
        }

        int availableStock = getAvailableStock(variant);
        if (availableStock <= 0) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_STOCK, "Sản phẩm đã hết hàng");
        }

        if (request.getQuantity() > availableStock) {
            throw new BusinessException(
                    ErrorCode.INSUFFICIENT_STOCK,
                    String.format("Số lượng yêu cầu (%d) vượt quá tồn kho khả dụng (tối đa: %d).",
                            request.getQuantity(), availableStock)
            );
        }

        item.setQuantity(request.getQuantity());
        cartItemRepository.save(item);

        return mapToCartResponse(cart);
    }

    @Override
    @Transactional
    public CartResponse removeCartItem(Long userId, String sessionId, Long itemId) {
        if (itemId == null || itemId <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Mã dòng sản phẩm giỏ hàng không hợp lệ");
        }

        Cart cart = findCart(userId, sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CART_NOT_FOUND, "Giỏ hàng không tồn tại"));

        CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CART_ITEM_NOT_FOUND, "Sản phẩm không có trong giỏ hàng"));

        if (!Objects.equals(item.getCart().getId(), cart.getId())) {
            throw new BusinessException(ErrorCode.CART_ITEM_NOT_FOUND, "Sản phẩm không thuộc giỏ hàng hiện tại");
        }

        cartItemRepository.delete(item);
        if (cart.getItems() != null) {
            cart.getItems().removeIf(ci -> Objects.equals(ci.getId(), itemId));
        }

        return mapToCartResponse(cart);
    }

    @Override
    @Transactional(readOnly = true)
    public CartValidationResponse validateCartStock(Long userId, String sessionId) {
        Optional<Cart> cartOpt = findCart(userId, sessionId);
        if (cartOpt.isEmpty()) {
            return new CartValidationResponse(true, List.of());
        }
        Cart cart = cartOpt.get();
        List<CartItem> items = cartItemRepository.findByCartId(cart.getId());
        if (items.isEmpty()) {
            return new CartValidationResponse(true, List.of());
        }

        List<CartItemStockIssueResponse> issues = new ArrayList<>();
        for (CartItem item : items) {
            ProductVariant variant = item.getVariant();
            if (variant == null || variant.isDeleted()) {
                issues.add(new CartItemStockIssueResponse(
                        item.getId(),
                        variant != null ? variant.getId() : null,
                        variant != null && variant.getProduct() != null ? variant.getProduct().getName() : "Sản phẩm",
                        variant != null ? variant.getSku() : null,
                        item.getQuantity(),
                        0,
                        StockIssueType.INACTIVE_OR_DELETED,
                        "Biến thể sản phẩm không tồn tại hoặc đã bị xoá"
                ));
                continue;
            }

            Product product = variant.getProduct();
            if (product == null || product.isDeleted() || product.getStatus() != ProductStatus.ACTIVE) {
                issues.add(new CartItemStockIssueResponse(
                        item.getId(),
                        variant.getId(),
                        product != null ? product.getName() : "Sản phẩm",
                        variant.getSku(),
                        item.getQuantity(),
                        0,
                        StockIssueType.INACTIVE_OR_DELETED,
                        "Sản phẩm đã ngừng kinh doanh"
                ));
                continue;
            }

            int availableStock = getAvailableStock(variant);
            if (availableStock <= 0) {
                issues.add(new CartItemStockIssueResponse(
                        item.getId(),
                        variant.getId(),
                        product.getName(),
                        variant.getSku(),
                        item.getQuantity(),
                        0,
                        StockIssueType.OUT_OF_STOCK,
                        String.format("Sản phẩm \"%s\" hiện đã hết hàng", product.getName())
                ));
            } else if (item.getQuantity() > availableStock) {
                issues.add(new CartItemStockIssueResponse(
                        item.getId(),
                        variant.getId(),
                        product.getName(),
                        variant.getSku(),
                        item.getQuantity(),
                        availableStock,
                        StockIssueType.INSUFFICIENT_STOCK,
                        String.format("Số lượng sản phẩm \"%s\" trong giỏ (%d) vượt quá tồn kho khả dụng (còn %d)",
                                product.getName(), item.getQuantity(), availableStock)
                ));
            }
        }

        boolean valid = issues.isEmpty();
        return new CartValidationResponse(valid, issues);
    }

    @Override
    @Transactional
    public CartSyncResponse syncCart(Long userId, String sessionId) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.INVALID_ACCESS_TOKEN, "Người dùng cần đăng nhập để đồng bộ giỏ hàng");
        }
        if (sessionId == null || sessionId.isBlank()) {
            CartResponse currentCart = getCart(userId, null);
            return new CartSyncResponse(currentCart, 0, false, "Không tìm thấy phiên giỏ hàng tạm để đồng bộ");
        }

        Optional<Cart> guestCartOpt = cartRepository.findBySessionId(sessionId);
        if (guestCartOpt.isEmpty()) {
            CartResponse currentCart = getCart(userId, null);
            return new CartSyncResponse(currentCart, 0, false, "Không có sản phẩm trong giỏ hàng tạm");
        }

        Cart guestCart = guestCartOpt.get();
        List<CartItem> guestItems = cartItemRepository.findByCartId(guestCart.getId());
        if (guestItems.isEmpty()) {
            cartRepository.delete(guestCart);
            CartResponse currentCart = getCart(userId, null);
            return new CartSyncResponse(currentCart, 0, false, "Giỏ hàng tạm trống");
        }

        Cart userCart = getOrCreateCart(userId, null);
        List<CartItem> userItems = cartItemRepository.findByCartId(userCart.getId());

        int mergedItemsCount = 0;
        boolean hasStockAdjusted = false;

        for (CartItem guestItem : guestItems) {
            ProductVariant variant = guestItem.getVariant();
            if (variant == null || variant.isDeleted()) {
                continue;
            }

            Product product = variant.getProduct();
            if (product == null || product.isDeleted() || product.getStatus() != ProductStatus.ACTIVE) {
                continue;
            }

            int availableStock = getAvailableStock(variant);
            if (availableStock <= 0) {
                continue;
            }

            Optional<CartItem> matchingUserItemOpt = userItems.stream()
                    .filter(ui -> ui.getVariant() != null && Objects.equals(ui.getVariant().getId(), variant.getId()))
                    .findFirst();

            if (matchingUserItemOpt.isPresent()) {
                CartItem userItem = matchingUserItemOpt.get();
                int combinedQty = userItem.getQuantity() + guestItem.getQuantity();
                if (combinedQty > availableStock) {
                    userItem.setQuantity(Math.max(1, availableStock));
                    hasStockAdjusted = true;
                } else {
                    userItem.setQuantity(combinedQty);
                }
                cartItemRepository.save(userItem);
            } else {
                int targetQty = guestItem.getQuantity();
                if (targetQty > availableStock) {
                    targetQty = Math.max(1, availableStock);
                    hasStockAdjusted = true;
                }
                CartItem newItem = new CartItem(userCart, variant, targetQty);
                cartItemRepository.save(newItem);
                userItems.add(newItem);
            }
            mergedItemsCount++;
        }

        cartItemRepository.deleteAll(guestItems);
        cartRepository.delete(guestCart);

        CartResponse updatedCartResponse = mapToCartResponse(userCart);
        String message = hasStockAdjusted
                ? String.format("Đã đồng bộ %d sản phẩm vào giỏ hàng (một số sản phẩm được điều chỉnh theo tồn kho tối đa)", mergedItemsCount)
                : String.format("Đã đồng bộ %d sản phẩm vào giỏ hàng thành công", mergedItemsCount);

        return new CartSyncResponse(updatedCartResponse, mergedItemsCount, hasStockAdjusted, message);
    }

    private Cart getOrCreateCart(Long userId, String sessionId) {
        Optional<Cart> cartOpt = findCart(userId, sessionId);
        if (cartOpt.isPresent()) {
            return cartOpt.get();
        }

        Cart newCart = new Cart();
        if (userId != null) {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Người dùng không tồn tại"));
            newCart.setUser(user);
        } else {
            newCart.setSessionId(sessionId != null && !sessionId.isBlank() ? sessionId : UUID.randomUUID().toString());
        }
        return cartRepository.save(newCart);
    }

    private Optional<Cart> findCart(Long userId, String sessionId) {
        if (userId != null) {
            return cartRepository.findByUserId(userId);
        }
        if (sessionId != null && !sessionId.isBlank()) {
            return cartRepository.findBySessionId(sessionId);
        }
        return Optional.empty();
    }

    private int getAvailableStock(ProductVariant variant) {
        Optional<Inventory> inventoryOpt = inventoryRepository.findByVariantId(variant.getId());
        if (inventoryOpt.isPresent()) {
            return inventoryOpt.get().getAvailableQuantity();
        }
        return variant.getStockQuantity() != null ? Math.max(0, variant.getStockQuantity()) : 0;
    }

    private CartResponse mapToCartResponse(Cart cart) {
        List<CartItem> items = cartItemRepository.findByCartId(cart.getId());
        List<CartItemResponse> itemResponses = new ArrayList<>();
        int totalItems = 0;
        BigDecimal subtotal = BigDecimal.ZERO;
        boolean cartHasStockIssue = false;

        for (CartItem item : items) {
            ProductVariant variant = item.getVariant();
            Product product = variant != null ? variant.getProduct() : null;
            int qty = item.getQuantity();
            int stock = variant != null ? getAvailableStock(variant) : 0;

            BigDecimal price = (variant != null && variant.getPrice() != null) ? variant.getPrice() : BigDecimal.ZERO;
            BigDecimal lineSubtotal = price.multiply(BigDecimal.valueOf(qty));

            totalItems += qty;
            subtotal = subtotal.add(lineSubtotal);

            String imageUrl = resolveVariantImageUrl(variant, product);

            boolean itemHasStockIssue = false;
            String stockStatusMessage = null;

            if (variant == null || variant.isDeleted() || product == null || product.isDeleted() || product.getStatus() != ProductStatus.ACTIVE) {
                itemHasStockIssue = true;
                stockStatusMessage = "Sản phẩm đã ngừng kinh doanh hoặc không tồn tại";
            } else if (stock <= 0) {
                itemHasStockIssue = true;
                stockStatusMessage = "Sản phẩm hiện đã hết hàng";
            } else if (qty > stock) {
                itemHasStockIssue = true;
                stockStatusMessage = String.format("Tồn kho không đủ (chỉ còn %d sản phẩm)", stock);
            }

            if (itemHasStockIssue) {
                cartHasStockIssue = true;
            }

            itemResponses.add(new CartItemResponse(
                    item.getId(),
                    variant != null ? variant.getId() : null,
                    product != null ? product.getId() : null,
                    product != null ? product.getName() : "Sản phẩm",
                    variant != null ? variant.getSku() : null,
                    variant != null ? variant.getColor() : null,
                    variant != null ? variant.getStorage() : null,
                    price,
                    variant != null ? variant.getOriginalPrice() : null,
                    imageUrl,
                    qty,
                    stock,
                    lineSubtotal,
                    itemHasStockIssue,
                    stockStatusMessage
            ));
        }

        BigDecimal shippingFee;
        if (totalItems == 0 || subtotal.compareTo(BigDecimal.ZERO) == 0) {
            shippingFee = BigDecimal.ZERO;
        } else if (subtotal.compareTo(FREE_SHIPPING_THRESHOLD) >= 0) {
            shippingFee = BigDecimal.ZERO;
        } else {
            shippingFee = DEFAULT_SHIPPING_FEE;
        }

        BigDecimal discountAmount = BigDecimal.ZERO;
        BigDecimal total = subtotal.add(shippingFee).subtract(discountAmount);

        boolean canCheckout = totalItems > 0 && !cartHasStockIssue;

        return new CartResponse(cart.getId(), totalItems, subtotal, shippingFee, discountAmount, total, cartHasStockIssue, canCheckout, itemResponses);
    }

    private String resolveVariantImageUrl(ProductVariant variant, Product product) {
        if (variant != null) {
            List<ProductImage> variantImages = productImageRepository.findByVariantId(variant.getId());
            if (!variantImages.isEmpty()) {
                return variantImages.get(0).getImageUrl();
            }
        }
        if (product != null) {
            List<ProductImage> productImages = productImageRepository.findByProductIdOrderByIsPrimaryDescDisplayOrderAscIdAsc(product.getId());
            if (!productImages.isEmpty()) {
                return productImages.get(0).getImageUrl();
            }
        }
        return null;
    }
}
