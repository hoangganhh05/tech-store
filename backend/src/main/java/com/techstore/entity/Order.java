package com.techstore.entity;

import com.techstore.enums.PaymentMethod;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.ArrayList;
import java.util.List;

/** Order persistence model. Order creation from the cart is implemented in US-08.4. */
@Entity
@Table(name = "orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "order_number", nullable = false, unique = true, length = 40)
    private String orderNumber;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voucher_id")
    private Voucher voucher;
    @Column(nullable = false, length = 25)
    private String status = "PENDING";
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 20)
    private PaymentMethod paymentMethod;
    @Column(name = "payment_status", nullable = false, length = 20)
    private String paymentStatus = "UNPAID";
    @Column(name = "note", length = 500)
    private String cancellationReason;
    @Column(name = "internal_note", length = 500)
    private String internalNote;
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal subtotal;
    @Column(name = "discount_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal discountAmount;
    @Column(name = "shipping_fee", nullable = false, precision = 15, scale = 2)
    private BigDecimal shippingFee;
    @Column(name = "total_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount;
    @Column(name = "placed_at", nullable = false, updatable = false)
    private Instant placedAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    @OneToOne(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private OrderAddress shippingAddress;
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("changedAt ASC, id ASC")
    private List<OrderStatusHistory> statusHistory = new ArrayList<>();

    protected Order() {}

    public Order(String orderNumber, User user, PaymentMethod paymentMethod,
                 BigDecimal subtotal, BigDecimal discountAmount, BigDecimal shippingFee) {
        this.orderNumber = Objects.requireNonNull(orderNumber);
        this.user = Objects.requireNonNull(user);
        this.paymentMethod = Objects.requireNonNull(paymentMethod, "Payment method is required");
        this.subtotal = Objects.requireNonNull(subtotal);
        this.discountAmount = Objects.requireNonNull(discountAmount);
        this.shippingFee = Objects.requireNonNull(shippingFee);
        this.totalAmount = subtotal.subtract(discountAmount).add(shippingFee);
        addStatusHistory(new OrderStatusHistory(this, status));
    }

    @PrePersist
    void onCreate() { placedAt = Instant.now(); updatedAt = placedAt; }
    @PreUpdate
    void onUpdate() { updatedAt = Instant.now(); }

    public Long getId() { return id; }
    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public String getPaymentStatus() { return paymentStatus; }
    public String getCancellationReason() { return cancellationReason; }
    public String getInternalNote() { return internalNote; }
    public String getOrderNumber() { return orderNumber; }
    public String getStatus() { return status; }
    public User getUser() { return user; }
    public BigDecimal getSubtotal() { return subtotal; }
    public BigDecimal getDiscountAmount() { return discountAmount; }
    public BigDecimal getShippingFee() { return shippingFee; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public Voucher getVoucher() { return voucher; }
    public void setVoucher(Voucher voucher) { this.voucher = voucher; }
    public Instant getPlacedAt() { return placedAt; }
    public List<OrderItem> getItems() { return items; }
    public OrderAddress getShippingAddress() { return shippingAddress; }
    public List<OrderStatusHistory> getStatusHistory() { return statusHistory; }
    public void setShippingAddress(OrderAddress address) { this.shippingAddress = address; }
    public void addItem(OrderItem item) { items.add(item); item.setOrder(this); }
    public void addStatusHistory(OrderStatusHistory entry) { statusHistory.add(entry); entry.setOrder(this); }
    public void setInternalNote(String internalNote) {
        this.internalNote = internalNote == null || internalNote.isBlank() ? null : internalNote.trim();
    }
    public void updateStatus(String status, User changedBy) {
        this.status = Objects.requireNonNull(status);
        addStatusHistory(new OrderStatusHistory(this, status, changedBy));
    }
    public void cancel(String reason) {
        this.cancellationReason = reason == null || reason.isBlank() ? null : reason.trim();
        updateStatus("CANCELLED", null);
    }
}
