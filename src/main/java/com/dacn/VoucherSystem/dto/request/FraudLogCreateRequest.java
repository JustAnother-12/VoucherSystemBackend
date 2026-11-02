package com.dacn.VoucherSystem.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record FraudLogCreateRequest(

        @Positive
        Long voucherId,

        @NotBlank
        @Size(max = 50)
        String customerId,

        @NotBlank
        @Size(max = 45)
        String ipAddress,

        @NotBlank
        @Size(max = 255)
        String deviceFingerprint,

        @NotBlank
        @Size(max = 255)
        String violationRule
) {
}
