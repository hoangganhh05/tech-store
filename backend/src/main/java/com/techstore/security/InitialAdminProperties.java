package com.techstore.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Credentials used only to provision the first production administrator.
 * Values are supplied by the deployment environment and must never be committed.
 */
@ConfigurationProperties(prefix = "app.initial-admin")
public class InitialAdminProperties {

    private String email;
    private String password;
    private String fullName = "Quản trị viên hệ thống";
    private String phone;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }
}
