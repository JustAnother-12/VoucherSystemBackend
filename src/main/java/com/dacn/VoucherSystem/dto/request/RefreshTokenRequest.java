package com.dacn.VoucherSystem.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

public record RefreshTokenRequest(
        @NotBlank(message = "Refresh token must not be empty")
        String refreshToken
) {
}
