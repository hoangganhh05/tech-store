package com.techstore.entity;

import com.techstore.enums.StockStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "inventories")
public class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id", nullable = false, unique = true)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private ProductVariant variant;

    @Column(name = "quantity_on_hand", nullable = false)
    private Integer quantityOnHand = 0;

    @Column(name = "quantity_reserved", nullable = false)
    private Integer quantityReserved = 0;

    @Column(name = "low_stock_threshold", nullable = false)
    private Integer lowStockThreshold = 5;

    @Version
    @Column(nullable = false)
    private Long version = 0L;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Inventory() {
    }

    public Inventory(ProductVariant variant, Integer quantityOnHand, Integer quantityReserved, Integer lowStockThreshold) {
        this.variant = variant;
        this.quantityOnHand = quantityOnHand != null ? quantityOnHand : 0;
        this.quantityReserved = quantityReserved != null ? quantityReserved : 0;
        this.lowStockThreshold = lowStockThreshold != null ? lowStockThreshold : 5;
    }

    @PrePersist
    @PreUpdate
    protected void onPersist() {
        this.updatedAt = Instant.now();
    }

    public int getAvailableQuantity() {
        int onHand = quantityOnHand != null ? quantityOnHand : 0;
        int reserved = quantityReserved != null ? quantityReserved : 0;
        return Math.max(0, onHand - reserved);
    }

    public StockStatus getStockStatus() {
        int available = getAvailableQuantity();
        if (available <= 0) {
            return StockStatus.OUT_OF_STOCK;
        }
        int threshold = lowStockThreshold != null ? lowStockThreshold : 5;
        if (available <= threshold) {
            return StockStatus.LOW_STOCK;
        }
        return StockStatus.IN_STOCK;
    }

    public Long getId() {
        return id;
    }

    public ProductVariant getVariant() {
        return variant;
    }

    public void setVariant(ProductVariant variant) {
        this.variant = variant;
    }

    public Integer getQuantityOnHand() {
        return quantityOnHand;
    }

    public void setQuantityOnHand(Integer quantityOnHand) {
        this.quantityOnHand = quantityOnHand;
    }

    public Integer getQuantityReserved() {
        return quantityReserved;
    }

    public void setQuantityReserved(Integer quantityReserved) {
        this.quantityReserved = quantityReserved;
    }

    public Integer getLowStockThreshold() {
        return lowStockThreshold;
    }

    public void setLowStockThreshold(Integer lowStockThreshold) {
        this.lowStockThreshold = lowStockThreshold;
    }

    public Long getVersion() {
        return version;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Inventory other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
