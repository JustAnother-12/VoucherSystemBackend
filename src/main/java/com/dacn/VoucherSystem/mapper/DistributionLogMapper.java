package com.dacn.VoucherSystem.mapper;

import com.dacn.VoucherSystem.dto.response.DistributionLogResponse;
import com.dacn.VoucherSystem.entity.DistributionLog;
import org.springframework.stereotype.Component;

@Component
public class DistributionLogMapper {
    public DistributionLogResponse toDistributionLogResponse(DistributionLog distributionLog) {
        if (distributionLog == null) {
            return null;
        }
        return DistributionLogResponse.builder()
                .id(distributionLog.getId())
                .voucherId(distributionLog.getVoucher().getId())
                .channel(distributionLog.getChannel())
                .status(distributionLog.getStatus())
                .sentAt(distributionLog.getSentAt())
                .targetAddress(distributionLog.getTargetAddress())
                .build();
    }
}
