package com.dacn.VoucherSystem.dto.response;

import com.dacn.VoucherSystem.entity.Voucher;
import com.dacn.VoucherSystem.enums.RedemptionStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
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
public class RedemptionLogResponse {
    private Long id;
    private Long voucherId;
    private String customerId;
    private String ipAddress;
    private String deviceFingerprint;
    private BigDecimal originalAmount;
    private BigDecimal discountAmount;
    private BigDecimal finalAmount;
    private RedemptionStatus status;
    private String failureReason;
    private LocalDateTime createAt;
}
