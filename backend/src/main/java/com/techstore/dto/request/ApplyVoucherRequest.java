package com.techstore.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ApplyVoucherRequest(
        @NotBlank(message = "Vui lòng nhập mã voucher")
        @Size(max = 50, message = "Mã voucher không được vượt quá 50 ký tự")
        String code
) {}
