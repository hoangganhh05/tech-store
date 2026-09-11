package com.techstore.dto.request;

import com.techstore.enums.ProductStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ProductCreateRequest(
        @NotBlank(message = "Tên sản phẩm không được để trống")
        @Size(max = 255, message = "Tên sản phẩm tối đa 255 ký tự")
        String name,

        @Size(max = 10000, message = "Mô tả sản phẩm tối đa 10000 ký tự")
        String description,

        @NotNull(message = "Thương hiệu không được để trống")
        Long brandId,

        @NotNull(message = "Danh mục không được để trống")
        Long categoryId,

        ProductStatus status
) {
}

