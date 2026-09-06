package com.techstore.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BrandRequest(
        @NotBlank(message = "Tên thương hiệu không được để trống")
        @Size(max = 100, message = "Tên thương hiệu không được vượt quá 100 ký tự")
        String name,

        @Size(max = 255, message = "Đường dẫn logo không được vượt quá 255 ký tự")
        String logoUrl,

        @Size(max = 2000, message = "Mô tả không được vượt quá 2000 ký tự")
        String description
) {
    public BrandRequest {
        if (name != null) {
            name = name.trim();
        }
        if (logoUrl != null) {
            logoUrl = logoUrl.trim();
            if (logoUrl.isEmpty()) {
                logoUrl = null;
            }
        }
        if (description != null) {
            description = description.trim();
            if (description.isEmpty()) {
                description = null;
            }
        }
    }
}
