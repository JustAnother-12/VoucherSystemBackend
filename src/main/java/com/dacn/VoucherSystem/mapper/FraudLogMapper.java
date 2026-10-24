package com.dacn.VoucherSystem.mapper;

import com.dacn.VoucherSystem.dto.response.FraudLogResponse;
import com.dacn.VoucherSystem.dto.response.RateLimitConfigResponse;
import com.dacn.VoucherSystem.entity.FraudLog;
import com.dacn.VoucherSystem.entity.RateLimitConfig;
import org.springframework.stereotype.Component;

@Component
public class FraudLogMapper {
    public FraudLogResponse toFraudLogResponse(FraudLog fraudLog) {
        if (fraudLog == null) {
            return null;
        }
        return FraudLogResponse.builder()
                .id(fraudLog.getId())
                .customerId(fraudLog.getCustomerId())
                .deviceFingerprint(fraudLog.getDeviceFingerprint())
                .ipAddress(fraudLog.getIpAddress())
                .violationRule(fraudLog.getViolationRule())
                .voucherId(fraudLog.getVoucher().getId())
                .build();
    }
}
