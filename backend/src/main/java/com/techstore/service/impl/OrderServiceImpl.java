package com.techstore.service.impl;

import com.techstore.dto.request.OrderInventoryDeductionRequest;
import com.techstore.dto.request.OrderInventoryRestoreRequest;
import com.techstore.dto.request.OrderItemStockRequest;
import com.techstore.dto.request.PlaceOrderRequest;
import com.techstore.dto.request.CancelOrderRequest;
import com.techstore.dto.request.UpdateOrderStatusRequest;
import com.techstore.dto.response.CartResponse;
import com.techstore.dto.response.OrderHistoryResponse;
import com.techstore.dto.response.PageResponse;
import com.techstore.dto.response.PlacedOrderItemResponse;
import com.techstore.dto.response.PlacedOrderResponse;
import com.techstore.dto.response.OrderCancellationResponse;
import com.techstore.dto.response.AdminOrderSummaryResponse;
import com.techstore.dto.response.AdminOrderDetailResponse;
import com.techstore.entity.*;
import com.techstore.enums.ErrorCode;
import com.techstore.enums.RoleCode;
import com.techstore.event.OrderPlacedEvent;
import com.techstore.exception.BusinessException;
import com.techstore.repository.*;
import com.techstore.service.CartService;
import com.techstore.service.InventoryService;
import com.techstore.service.OrderService;
import com.techstore.service.VoucherRedemption;
import com.techstore.service.VoucherService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Map;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class OrderServiceImpl implements OrderService {
    private static final int MAX_HISTORY_PAGE_SIZE = 50;
    private static final Set<String> ORDER_STATUSES = Set.of(
            "PENDING", "CONFIRMED", "SHIPPING", "COMPLETED", "CANCELLED"
    );
    private static final Map<String, Set<String>> ALLOWED_STATUS_TRANSITIONS = Map.of(
            "PENDING", Set.of("CONFIRMED", "CANCELLED"),
            "CONFIRMED", Set.of("SHIPPING", "CANCELLED"),
            "SHIPPING", Set.of("COMPLETED"),
            "COMPLETED", Set.of(),
            "CANCELLED", Set.of()
    );

    private final UserRepository users; private final AddressRepository addresses; private final CartRepository carts;
    private final CartItemRepository cartItems; private final OrderRepository orders; private final CartService cartService;
    private final InventoryService inventory; private final ApplicationEventPublisher eventPublisher;
    private final VoucherService voucherService;
    @org.springframework.beans.factory.annotation.Autowired
    public OrderServiceImpl(UserRepository users, AddressRepository addresses, CartRepository carts, CartItemRepository cartItems,
                            OrderRepository orders, CartService cartService, InventoryService inventory,
                            ApplicationEventPublisher eventPublisher, VoucherService voucherService) {
        this.users=users; this.addresses=addresses; this.carts=carts; this.cartItems=cartItems; this.orders=orders; this.cartService=cartService; this.inventory=inventory; this.eventPublisher=eventPublisher;
        this.voucherService = voucherService;
    }

    public OrderServiceImpl(UserRepository users, AddressRepository addresses, CartRepository carts, CartItemRepository cartItems,
                            OrderRepository orders, CartService cartService, InventoryService inventory,
                            ApplicationEventPublisher eventPublisher) {
        this(users, addresses, carts, cartItems, orders, cartService, inventory, eventPublisher, null);
    }

    @Override @Transactional
    public PlacedOrderResponse placeOrder(Long userId, PlaceOrderRequest request) {
        User user = users.findById(userId).orElseThrow(() -> new BusinessException(ErrorCode.INVALID_ACCESS_TOKEN, "Phiên đăng nhập không hợp lệ hoặc đã hết hạn"));
        Address address = addresses.findByIdAndUserId(request.addressId(), userId).orElseThrow(() -> new BusinessException(ErrorCode.ADDRESS_NOT_FOUND, "Địa chỉ giao hàng không tồn tại hoặc không thuộc tài khoản hiện tại"));
        CartResponse cart = cartService.getCart(userId, null);
        if (cart.id() == null || cart.items().isEmpty()) throw new BusinessException(ErrorCode.CART_NOT_FOUND, "Giỏ hàng không có sản phẩm để đặt hàng");
        if (!cart.canCheckout()) {
            String issues = cart.items().stream().filter(i -> i.hasStockIssue()).map(i -> i.productName() + ": " + i.stockStatusMessage()).reduce((a,b) -> a + "; " + b).orElse("Tồn kho không đủ");
            throw new BusinessException(ErrorCode.INSUFFICIENT_STOCK, issues);
        }
        VoucherRedemption redemption = null;
        if (request.voucherCode() != null && !request.voucherCode().isBlank()) {
            if (voucherService == null) throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Voucher service chưa được cấu hình");
            redemption = voucherService.redeem(userId, cart, request.voucherCode());
            cart = redemption.cart();
        }
        String number = "TS-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
        Order order = new Order(number, user, request.paymentMethod(), cart.subtotal(), cart.discountAmount(), cart.shippingFee());
        if (redemption != null) order.setVoucher(redemption.voucher());
        order.setShippingAddress(new OrderAddress(order, address.getRecipientName(), address.getPhone(), address.getStreetAddress(), address.getWard(), address.getDistrict(), address.getProvince()));
        List<OrderItemStockRequest> deductions = new ArrayList<>();
        List<PlacedOrderItemResponse> confirmationItems = new ArrayList<>();
        cart.items().forEach(i -> {
            String label = (i.color() == null ? "" : i.color()) + (i.storage() == null ? "" : " / " + i.storage());
            order.addItem(new OrderItem(i.variantId(), i.productName(), i.sku(), label, i.price(), i.quantity()));
            deductions.add(new OrderItemStockRequest(i.variantId(), i.quantity()));
            confirmationItems.add(new PlacedOrderItemResponse(i.productName(), label, i.price(), i.quantity(), i.subtotal()));
        });
        orders.saveAndFlush(order);
        if (redemption != null) voucherService.recordUsage(user, order, redemption);
        inventory.deductInventoryForOrder(userId, new OrderInventoryDeductionRequest(order.getId(), order.getOrderNumber(), deductions, null));
        cartItems.deleteByCartId(cart.id());
        Instant placedAt = order.getPlacedAt() == null ? Instant.now() : order.getPlacedAt();
        String estimatedProcessingTime = "1-2 ngày làm việc";
        List<PlacedOrderItemResponse> immutableItems = List.copyOf(confirmationItems);
        eventPublisher.publishEvent(new OrderPlacedEvent(user.getEmail(), user.getFullName(), order.getOrderNumber(),
                order.getTotalAmount(), placedAt, estimatedProcessingTime, immutableItems));
        return new PlacedOrderResponse(order.getId(), order.getOrderNumber(), order.getStatus(), order.getTotalAmount(),
                placedAt, estimatedProcessingTime, immutableItems);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<OrderHistoryResponse> getMyOrders(Long userId, String status, int page, int size) {
        if (page < 0) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Số trang phải lớn hơn hoặc bằng 0");
        }
        if (size < 1 || size > MAX_HISTORY_PAGE_SIZE) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "Kích thước trang phải nằm trong khoảng từ 1 đến " + MAX_HISTORY_PAGE_SIZE);
        }

        String normalizedStatus = normalizeStatus(status);
        Sort newestFirst = Sort.by(
                Sort.Order.desc("placedAt"),
                Sort.Order.desc("id")
        );
        PageRequest pageable = PageRequest.of(page, size, newestFirst);
        Page<Order> ordersPage = normalizedStatus == null
                ? orders.findByUserId(userId, pageable)
                : orders.findByUserIdAndStatus(userId, normalizedStatus, pageable);
        return PageResponse.of(ordersPage.map(OrderHistoryResponse::from));
    }

    @Override
    @Transactional(readOnly = true)
    public com.techstore.dto.response.OrderDetailResponse getOrderDetail(Long userId, Long orderId) {
        Order order = orders.findById(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND, "Đơn hàng không tồn tại"));
        User requester = users.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_ACCESS_TOKEN, "Phiên đăng nhập không hợp lệ hoặc đã hết hạn"));
        boolean isOwner = order.getUser().getId().equals(userId);
        boolean isAdmin = requester.getRoleCodes().contains(RoleCode.ADMIN);
        if (!isOwner && !isAdmin) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED, "Bạn không có quyền xem đơn hàng này");
        }
        return com.techstore.dto.response.OrderDetailResponse.from(order);
    }

    @Override
    @Transactional
    public OrderCancellationResponse cancelOrder(Long userId, Long orderId, CancelOrderRequest request) {
        Order order = orders.findByIdForUpdate(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND, "Đơn hàng không tồn tại"));
        if (!order.getUser().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED, "Bạn không có quyền huỷ đơn hàng này");
        }
        if (!Set.of("PENDING", "CONFIRMED").contains(order.getStatus())) {
            throw new BusinessException(ErrorCode.ORDER_CANNOT_CANCEL,
                    "Chỉ có thể huỷ đơn hàng đang chờ xác nhận hoặc đã xác nhận");
        }

        String reason = request == null || request.reason() == null || request.reason().isBlank()
                ? "Khách hàng yêu cầu huỷ đơn hàng"
                : request.reason().trim();
        List<OrderItemStockRequest> items = order.getItems().stream()
                .map(item -> new OrderItemStockRequest(item.getVariantId(), item.getQuantity()))
                .collect(Collectors.toList());
        inventory.restoreInventoryForOrder(userId,
                new OrderInventoryRestoreRequest(order.getId(), order.getOrderNumber(), items, reason));
        order.cancel(reason);
        orders.saveAndFlush(order);
        return new OrderCancellationResponse(order.getId(), order.getOrderNumber(), order.getStatus(), order.getCancellationReason());
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AdminOrderSummaryResponse> getAdminOrders(
            String search,
            String status,
            LocalDate fromDate,
            LocalDate toDate,
            int page,
            int size
    ) {
        if (page < 0) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Số trang phải lớn hơn hoặc bằng 0");
        }
        if (size < 1 || size > MAX_HISTORY_PAGE_SIZE) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "Kích thước trang phải nằm trong khoảng từ 1 đến " + MAX_HISTORY_PAGE_SIZE);
        }
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "Ngày bắt đầu không được sau ngày kết thúc");
        }

        String normalizedSearch = normalizeSearch(search);
        String normalizedStatus = normalizeStatus(status);
        Instant fromInclusive = fromDate == null ? null : fromDate.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant toExclusive = toDate == null ? null : toDate.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Order.desc("placedAt"), Sort.Order.desc("id")));
        Page<Order> ordersPage = orders.findAdminOrders(normalizedSearch, normalizedStatus, fromInclusive, toExclusive, pageable);
        return PageResponse.of(ordersPage.map(AdminOrderSummaryResponse::from));
    }

    @Override
    @Transactional(readOnly = true)
    public AdminOrderDetailResponse getAdminOrderDetail(Long orderId) {
        if (orderId == null || orderId < 1) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Mã đơn hàng không hợp lệ");
        }
        Order order = orders.findById(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND, "Đơn hàng không tồn tại"));
        return AdminOrderDetailResponse.from(order);
    }

    @Override
    @Transactional
    public AdminOrderDetailResponse updateAdminOrderStatus(
            Long adminUserId,
            Long orderId,
            UpdateOrderStatusRequest request
    ) {
        if (orderId == null || orderId < 1) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Mã đơn hàng không hợp lệ");
        }
        if (request == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Trạng thái đơn hàng không được để trống");
        }

        String nextStatus = normalizeStatus(request.status());
        if (nextStatus == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Trạng thái đơn hàng không được để trống");
        }

        User admin = users.findById(adminUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_ACCESS_TOKEN,
                        "Phiên đăng nhập không hợp lệ hoặc đã hết hạn"));
        Order order = orders.findByIdForUpdate(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND, "Đơn hàng không tồn tại"));
        Set<String> allowedStatuses = ALLOWED_STATUS_TRANSITIONS.getOrDefault(order.getStatus(), Set.of());
        if (!allowedStatuses.contains(nextStatus)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "Không thể chuyển trạng thái từ " + order.getStatus() + " sang " + nextStatus);
        }

        order.updateStatus(nextStatus, admin);
        orders.saveAndFlush(order);
        return AdminOrderDetailResponse.from(order);
    }

    private String normalizeSearch(String search) {
        if (search == null || search.isBlank()) return null;
        String normalized = search.trim();
        if (normalized.length() > 100) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Từ khoá tìm kiếm không được vượt quá 100 ký tự");
        }
        return normalized;
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) return null;
        String normalized = status.trim().toUpperCase(Locale.ROOT);
        if (!ORDER_STATUSES.contains(normalized)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "Trạng thái đơn hàng không hợp lệ: " + status);
        }
        return normalized;
    }
}
