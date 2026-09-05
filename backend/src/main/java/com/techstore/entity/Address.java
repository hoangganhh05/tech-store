package com.techstore.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.util.Objects;

@Entity
@Table(name = "addresses")
public class Address extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "recipient_name", nullable = false, length = 150)
    private String recipientName;

    @Column(nullable = false, length = 20)
    private String phone;

    @Column(nullable = false, length = 100)
    private String province;

    @Column(nullable = false, length = 100)
    private String district;

    @Column(nullable = false, length = 100)
    private String ward;

    @Column(name = "street_address", nullable = false, length = 255)
    private String streetAddress;

    @Column(name = "is_default", nullable = false)
    private boolean isDefault = false;

    protected Address() {
    }

    public Address(User user, String recipientName, String phone,
                   String province, String district, String ward,
                   String streetAddress) {
        this.user = Objects.requireNonNull(user, "user must not be null");
        this.recipientName = Objects.requireNonNull(recipientName, "recipientName must not be null");
        this.phone = Objects.requireNonNull(phone, "phone must not be null");
        this.province = Objects.requireNonNull(province, "province must not be null");
        this.district = Objects.requireNonNull(district, "district must not be null");
        this.ward = Objects.requireNonNull(ward, "ward must not be null");
        this.streetAddress = Objects.requireNonNull(streetAddress, "streetAddress must not be null");
    }

    public void update(String recipientName, String phone,
                       String province, String district, String ward,
                       String streetAddress) {
        this.recipientName = Objects.requireNonNull(recipientName, "recipientName must not be null");
        this.phone = Objects.requireNonNull(phone, "phone must not be null");
        this.province = Objects.requireNonNull(province, "province must not be null");
        this.district = Objects.requireNonNull(district, "district must not be null");
        this.ward = Objects.requireNonNull(ward, "ward must not be null");
        this.streetAddress = Objects.requireNonNull(streetAddress, "streetAddress must not be null");
    }

    public void markAsDefault() {
        this.isDefault = true;
    }

    public void unmarkAsDefault() {
        this.isDefault = false;
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public String getRecipientName() {
        return recipientName;
    }

    public String getPhone() {
        return phone;
    }

    public String getProvince() {
        return province;
    }

    public String getDistrict() {
        return district;
    }

    public String getWard() {
        return ward;
    }

    public String getStreetAddress() {
        return streetAddress;
    }

    public boolean isDefault() {
        return isDefault;
    }
}
