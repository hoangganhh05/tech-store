package com.techstore.entity;

import com.techstore.enums.DiscountType;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "vouchers")
public class Voucher extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;
    @Column(nullable = false, length = 150)
    private String name;
    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false, length = 10)
    private DiscountType discountType;
    @Column(name = "discount_value", nullable = false, precision = 15, scale = 2)
    private BigDecimal discountValue;
    @Column(name = "max_discount", precision = 15, scale = 2)
    private BigDecimal maxDiscount;
    @Column(name = "minimum_order", nullable = false, precision = 15, scale = 2)
    private BigDecimal minimumOrder = BigDecimal.ZERO;
    @Column(name = "usage_limit")
    private Integer usageLimit;
    @Column(name = "per_user_limit", nullable = false)
    private Integer perUserLimit = 1;
    @Column(name = "used_count", nullable = false)
    private Integer usedCount = 0;
    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;
    @Column(name = "ends_at", nullable = false)
    private Instant endsAt;
    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    protected Voucher() {}

    public Voucher(String code, String name, DiscountType discountType, BigDecimal discountValue,
                   BigDecimal maxDiscount, BigDecimal minimumOrder, Integer usageLimit,
                   Integer perUserLimit, Instant startsAt, Instant endsAt, boolean active) {
        this.code = Objects.requireNonNull(code).trim().toUpperCase();
        this.name = Objects.requireNonNull(name);
        this.discountType = Objects.requireNonNull(discountType);
        this.discountValue = Objects.requireNonNull(discountValue);
        this.maxDiscount = maxDiscount;
        this.minimumOrder = minimumOrder == null ? BigDecimal.ZERO : minimumOrder;
        this.usageLimit = usageLimit;
        this.perUserLimit = perUserLimit == null ? 1 : perUserLimit;
        this.startsAt = Objects.requireNonNull(startsAt);
        this.endsAt = Objects.requireNonNull(endsAt);
        this.active = active;
    }

    public Long getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public DiscountType getDiscountType() { return discountType; }
    public BigDecimal getDiscountValue() { return discountValue; }
    public BigDecimal getMaxDiscount() { return maxDiscount; }
    public BigDecimal getMinimumOrder() { return minimumOrder; }
    public Integer getUsageLimit() { return usageLimit; }
    public Integer getPerUserLimit() { return perUserLimit; }
    public Integer getUsedCount() { return usedCount; }
    public Instant getStartsAt() { return startsAt; }
    public Instant getEndsAt() { return endsAt; }
    public boolean isActive() { return active; }
    public void incrementUsedCount() { usedCount = (usedCount == null ? 0 : usedCount) + 1; }

    public void update(String code, String name, DiscountType discountType, BigDecimal discountValue,
                       BigDecimal maxDiscount, BigDecimal minimumOrder, Integer usageLimit,
                       Integer perUserLimit, Instant startsAt, Instant endsAt, boolean active) {
        this.code = Objects.requireNonNull(code).trim().toUpperCase();
        this.name = Objects.requireNonNull(name).trim();
        this.discountType = Objects.requireNonNull(discountType);
        this.discountValue = Objects.requireNonNull(discountValue);
        this.maxDiscount = maxDiscount;
        this.minimumOrder = minimumOrder == null ? BigDecimal.ZERO : minimumOrder;
        this.usageLimit = usageLimit;
        this.perUserLimit = perUserLimit == null ? 1 : perUserLimit;
        this.startsAt = Objects.requireNonNull(startsAt);
        this.endsAt = Objects.requireNonNull(endsAt);
        this.active = active;
    }
}
