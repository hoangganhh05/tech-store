package com.techstore.entity;

import com.techstore.enums.VariantStatus;
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
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.util.Objects;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(
        name = "product_variants",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_product_variants_sku", columnNames = {"sku"})
        }
)
public class ProductVariant extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Product product;

    @Column(nullable = false, unique = true, length = 100)
    private String sku;

    @Column(length = 50)
    private String color;

    @Column(length = 50)
    private String storage;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal price;

    @Column(name = "original_price", precision = 15, scale = 2)
    private BigDecimal originalPrice;

    @Column(name = "stock_quantity", nullable = false)
    private Integer stockQuantity = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private VariantStatus status = VariantStatus.ACTIVE;

    protected ProductVariant() {
    }

    public ProductVariant(Product product, String sku, String color, String storage,
                          BigDecimal price, BigDecimal originalPrice, Integer stockQuantity, VariantStatus status) {
        this.product = Objects.requireNonNull(product, "product must not be null");
        this.sku = Objects.requireNonNull(sku, "sku must not be null").trim().toUpperCase();
        this.color = color != null ? color.trim() : null;
        this.storage = storage != null ? storage.trim() : null;
        this.price = Objects.requireNonNull(price, "price must not be null");
        this.originalPrice = originalPrice;
        this.stockQuantity = stockQuantity != null ? stockQuantity : 0;
        this.status = status != null ? status : VariantStatus.ACTIVE;
    }

    public void update(String sku, String color, String storage,
                       BigDecimal price, BigDecimal originalPrice, Integer stockQuantity, VariantStatus status) {
        this.sku = Objects.requireNonNull(sku, "sku must not be null").trim().toUpperCase();
        this.color = color != null ? color.trim() : null;
        this.storage = storage != null ? storage.trim() : null;
        this.price = Objects.requireNonNull(price, "price must not be null");
        this.originalPrice = originalPrice;
        if (stockQuantity != null) {
            this.stockQuantity = stockQuantity;
        }
        if (status != null) {
            this.status = status;
        }
    }

    public Long getId() {
        return id;
    }

    public Product getProduct() {
        return product;
    }

    public String getSku() {
        return sku;
    }

    public String getColor() {
        return color;
    }

    public String getStorage() {
        return storage;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public BigDecimal getOriginalPrice() {
        return originalPrice;
    }

    public Integer getStockQuantity() {
        return stockQuantity;
    }

    public void setStockQuantity(Integer stockQuantity) {
        this.stockQuantity = stockQuantity != null ? stockQuantity : 0;
    }

    public VariantStatus getStatus() {
        return status;
    }

    public void setStatus(VariantStatus status) {
        this.status = Objects.requireNonNull(status, "status must not be null");
    }
}
