package com.dacn.VoucherSystem.service.interfaces;

import com.dacn.VoucherSystem.dto.response.DistributionLogResponse;
import com.dacn.VoucherSystem.enums.DistributionStatus;

import java.util.List;

public interface DistributionLogService {
    String EMAIL = "EMAIL";
    String WEBHOOK = "WEBHOOK";

    String ALLOWED_CHANNELS = EMAIL + "|" + WEBHOOK;

    List<DistributionLogResponse> getAllDistributionLog(
            Long voucherId,
            Long campaignId,
            DistributionStatus status
    );

    DistributionLogResponse getDistributionLogById(Long id);

    DistributionLogResponse createDistribution(
            Long batchId,
            Long campaignId,
            String channel,
            String targetAddress
    );
}
