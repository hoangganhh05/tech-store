package com.techstore.entity;

import com.techstore.enums.ProductStatus;
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

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(
        name = "products",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_product_name_brand", columnNames = {"name", "brand_id"})
        }
)
public class Product extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id", nullable = false)
    private Brand brand;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ProductStatus status = ProductStatus.DRAFT;

    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted = false;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    protected Product() {
    }

    public Product(String name, String description, Brand brand, Category category, ProductStatus status) {
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.description = description;
        this.brand = Objects.requireNonNull(brand, "brand must not be null");
        this.category = Objects.requireNonNull(category, "category must not be null");
        this.status = status != null ? status : ProductStatus.DRAFT;
        this.isDeleted = false;
    }

    public void update(String name, String description, Brand brand, Category category, ProductStatus status) {
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.description = description;
        this.brand = Objects.requireNonNull(brand, "brand must not be null");
        this.category = Objects.requireNonNull(category, "category must not be null");
        if (status != null) {
            this.status = status;
        }
    }

    public void softDelete() {
        this.isDeleted = true;
        this.deletedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Brand getBrand() {
        return brand;
    }

    public Category getCategory() {
        return category;
    }

    public ProductStatus getStatus() {
        return status;
    }

    public void setStatus(ProductStatus status) {
        this.status = Objects.requireNonNull(status, "status must not be null");
    }

    public boolean isDeleted() {
        return isDeleted;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }
}
