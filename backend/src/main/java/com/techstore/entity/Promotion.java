package com.techstore.entity;

import com.techstore.enums.PromotionTargetType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "promotions")
public class Promotion extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 20)
    private PromotionTargetType targetType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id")
    private ProductVariant variant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(name = "discount_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal discountPercent;

    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;

    @Column(name = "ends_at", nullable = false)
    private Instant endsAt;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    protected Promotion() {
    }

    public Promotion(String name, PromotionTargetType targetType, Product product, ProductVariant variant,
                     Category category, BigDecimal discountPercent, Instant startsAt, Instant endsAt, boolean active) {
        update(name, targetType, product, variant, category, discountPercent, startsAt, endsAt, active);
    }

    public void update(String name, PromotionTargetType targetType, Product product, ProductVariant variant,
                       Category category, BigDecimal discountPercent, Instant startsAt, Instant endsAt, boolean active) {
        this.name = Objects.requireNonNull(name, "name must not be null").trim();
        this.targetType = Objects.requireNonNull(targetType, "targetType must not be null");
        this.product = product;
        this.variant = variant;
        this.category = category;
        this.discountPercent = Objects.requireNonNull(discountPercent, "discountPercent must not be null");
        this.startsAt = Objects.requireNonNull(startsAt, "startsAt must not be null");
        this.endsAt = Objects.requireNonNull(endsAt, "endsAt must not be null");
        this.active = active;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public PromotionTargetType getTargetType() { return targetType; }
    public Product getProduct() { return product; }
    public ProductVariant getVariant() { return variant; }
    public Category getCategory() { return category; }
    public BigDecimal getDiscountPercent() { return discountPercent; }
    public Instant getStartsAt() { return startsAt; }
    public Instant getEndsAt() { return endsAt; }
    public boolean isActive() { return active; }
}
