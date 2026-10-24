package com.dacn.VoucherSystem.mapper;

import com.dacn.VoucherSystem.dto.response.RedemptionLogResponse;
import com.dacn.VoucherSystem.entity.RedemptionLog;

public class RedemptionLogMapper {
    public RedemptionLogResponse toRedemptionLogResponse(RedemptionLog redemptionLog) {
        if (redemptionLog == null) {
            return null;
        }
        return RedemptionLogResponse.builder()
                .id(redemptionLog.getId())
                .voucherId(redemptionLog.getVoucher().getId())
                .customerId(redemptionLog.getCustomerId())
                .deviceFingerprint(redemptionLog.getDeviceFingerprint())
                .discountAmount(redemptionLog.getDiscountAmount())
                .failureReason(redemptionLog.getFailureReason())
                .finalAmount(redemptionLog.getFinalAmount())
                .ipAddress(redemptionLog.getIpAddress())
                .originalAmount(redemptionLog.getOriginalAmount())
                .status(redemptionLog.getStatus())
                .build();
    }
}
