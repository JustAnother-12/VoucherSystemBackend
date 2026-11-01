package com.dacn.VoucherSystem.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

public record LoginRequest(
        @NotBlank(message = "Username must not be empty")
        String username,
        @NotBlank(message = "Password must not be empty")
        String password
) {
}
