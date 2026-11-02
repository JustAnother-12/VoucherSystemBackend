package com.dacn.VoucherSystem.dto.response;

import com.dacn.VoucherSystem.entity.Voucher;
import com.dacn.VoucherSystem.enums.RedemptionStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
@JsonPropertyOrder({
        "id",
        "voucherId",
        "customerId",
        "ipAddress",
        "deviceFingerprint",
        "originalAmount",
        "discountAmount",
        "finalAmount",
        "status",
        "failureReason",
        "createAt"
})
public record RedemptionLogResponse(
        Long id,
        Long voucherId,
        String customerId,
        String ipAddress,
        String deviceFingerprint,
        BigDecimal originalAmount,
        BigDecimal discountAmount,
        BigDecimal finalAmount,
        RedemptionStatus status,
        String failureReason,
        LocalDateTime createAt
) {
}
