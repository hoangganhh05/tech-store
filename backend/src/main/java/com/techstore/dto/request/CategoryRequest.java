package com.techstore.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoryRequest(
        @NotBlank(message = "Tên danh mục không được để trống")
        @Size(max = 100, message = "Tên danh mục tối đa 100 ký tự")
        String name,

        String description,

        Long parentId,

        @Size(max = 255, message = "Đường dẫn ảnh tối đa 255 ký tự")
        String imageUrl
) {
}