package com.techstore.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "order_addresses")
public class OrderAddress {
    @Id @Column(name = "order_id") private Long orderId;
    @OneToOne(fetch = FetchType.LAZY, optional = false) @MapsId @JoinColumn(name = "order_id") private Order order;
    @Column(name = "recipient_name", nullable = false, length = 150) private String recipientName;
    @Column(name = "recipient_phone", nullable = false, length = 20) private String recipientPhone;
    @Column(name = "line1", nullable = false, length = 255) private String line1;
    @Column(name = "ward", length = 120) private String ward;
    @Column(name = "district", nullable = false, length = 120) private String district;
    @Column(name = "province", nullable = false, length = 120) private String province;

    protected OrderAddress() {}
    public OrderAddress(Order order, String recipientName, String recipientPhone, String line1,
                        String ward, String district, String province) {
        this.order = order; this.recipientName = recipientName; this.recipientPhone = recipientPhone;
        this.line1 = line1; this.ward = ward; this.district = district; this.province = province;
    }
}
