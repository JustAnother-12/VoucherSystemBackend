package com.dacn.VoucherSystem.dto.response;

import com.dacn.VoucherSystem.entity.Voucher;
import com.dacn.VoucherSystem.enums.RedemptionStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
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
}
