package com.dacn.VoucherSystem.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Builder
@JsonPropertyOrder({
        "id",
        "voucherId",
        "customerId",
        "ipAddress",
        "deviceFingerprint",
        "violationRule",
        "create_at"
})
public record FraudLogResponse(
        Long id,
        Long voucherId,
        String customerId,
        String ipAddress,
        String deviceFingerprint,
        String violationRule,
        LocalDateTime createAt
) {
}
