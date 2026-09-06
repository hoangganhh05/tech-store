package com.techstore.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProductSpecificationRequest(
        @NotBlank(message = "Tên thông số kỹ thuật không được để trống")
        @Size(max = 100, message = "Tên thông số kỹ thuật tối đa 100 ký tự")
        String specKey,

        @NotBlank(message = "Giá trị thông số kỹ thuật không được để trống")
        @Size(max = 500, message = "Giá trị thông số kỹ thuật tối đa 500 ký tự")
        String specValue,

        @Min(value = 0, message = "Thứ tự hiển thị phải lớn hơn hoặc bằng 0")
        Integer displayOrder
) {
}
