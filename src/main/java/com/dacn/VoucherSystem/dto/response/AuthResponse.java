package com.dacn.VoucherSystem.dto.response;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Builder
@JsonPropertyOrder({
        "accessToken",
        "refreshToken",
        "accessExpiresAt",
        "refreshExpiresAt",
        "tokenType"
})
public record AuthResponse(
        String accessToken,
        String refreshToken,
        LocalDateTime accessExpiresAt,
        LocalDateTime refreshExpiresAt,
        String tokenType
)
{
}
