package com.dacn.VoucherSystem.dto.response;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@JsonPropertyOrder({
        "accessToken",
        "refreshToken",
        "accessExpiresAt",
        "refreshExpiresAt",
        "tokenType"
})
public class AuthResponse {
    private String accessToken;
    private String refreshToken;
    private LocalDateTime accessExpiresAt;
    private LocalDateTime refreshExpiresAt;
    private String tokenType = "Bearer";
}
