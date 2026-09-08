package com.techstore.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "voucher_usages", uniqueConstraints = @UniqueConstraint(name = "uk_voucher_usage_order", columnNames = "order_id"))
public class VoucherUsage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "voucher_id", nullable = false)
    private Voucher voucher;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;
    @Column(name = "discount_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal discountAmount;
    @Column(name = "used_at", nullable = false)
    private Instant usedAt = Instant.now();

    protected VoucherUsage() {}
    public VoucherUsage(Voucher voucher, User user, Order order, BigDecimal discountAmount) {
        this.voucher = voucher; this.user = user; this.order = order; this.discountAmount = discountAmount;
    }
}
