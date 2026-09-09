package com.techstore.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.brand")
public class StoreBrandProperties {

    private String name = "Đăng Tùng Mobile";
    private String industry = "Kinh doanh phụ kiện điện thoại.";
    private String address = "Xóm Lê Lợi, Thôn Cập Thượng, Phường Nam Đồng, TP Hải Phòng";
    private String contactPhone = "0867116863";
    private String contactEmail = "hoanghd064@gmail.com";
    private String openingHours = "";
    private String zaloUrl = "";
    private String socialUrl = "";
    private String mapUrl = "";
    private String warrantyPolicyUrl = "";

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIndustry() {
        return industry;
    }

    public void setIndustry(String industry) {
        this.industry = industry;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public void setContactEmail(String contactEmail) {
        this.contactEmail = contactEmail;
    }

    public String getOpeningHours() {
        return openingHours;
    }

    public void setOpeningHours(String openingHours) {
        this.openingHours = openingHours;
    }

    public String getZaloUrl() {
        return zaloUrl;
    }

    public void setZaloUrl(String zaloUrl) {
        this.zaloUrl = zaloUrl;
    }

    public String getSocialUrl() {
        return socialUrl;
    }

    public void setSocialUrl(String socialUrl) {
        this.socialUrl = socialUrl;
    }

    public String getMapUrl() {
        return mapUrl;
    }

    public void setMapUrl(String mapUrl) {
        this.mapUrl = mapUrl;
    }

    public String getWarrantyPolicyUrl() {
        return warrantyPolicyUrl;
    }

    public void setWarrantyPolicyUrl(String warrantyPolicyUrl) {
        this.warrantyPolicyUrl = warrantyPolicyUrl;
    }
}
