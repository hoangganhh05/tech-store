package com.techstore.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "order_items")
public class OrderItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "order_id", nullable = false) private Order order;
    @Column(name = "variant_id", nullable = false) private Long variantId;
    @Column(name = "product_name", nullable = false, length = 255) private String productName;
    @Column(nullable = false, length = 80) private String sku;
    @Column(name = "variant_label", length = 180) private String variantLabel;
    @Column(name = "unit_price", nullable = false, precision = 15, scale = 2) private BigDecimal unitPrice;
    @Column(nullable = false) private Integer quantity;
    @Column(nullable = false, precision = 15, scale = 2) private BigDecimal subtotal;

    protected OrderItem() {}
    public OrderItem(Long variantId, String productName, String sku, String variantLabel,
                     BigDecimal unitPrice, Integer quantity) {
        this.variantId = variantId; this.productName = productName; this.sku = sku;
        this.variantLabel = variantLabel; this.unitPrice = unitPrice; this.quantity = quantity;
        this.subtotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
    public void setOrder(Order order) { this.order = order; }
    public Long getVariantId() { return variantId; }
    public Integer getQuantity() { return quantity; }
}
