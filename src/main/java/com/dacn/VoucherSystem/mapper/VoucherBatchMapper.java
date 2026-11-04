package com.dacn.VoucherSystem.mapper;

import com.dacn.VoucherSystem.dto.response.VoucherBatchResponse;
import com.dacn.VoucherSystem.entity.VoucherBatch;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
public class VoucherBatchMapper {
    public VoucherBatchResponse toVoucherBatchResponse(VoucherBatch batch) {
        return VoucherBatchResponse.builder()
                .id(batch.getId())
                .campaignId(batch.getCampaign().getId())
                .ruleId(batch.getRule().getId())
                .batchType(batch.getBatchType())
                .codeConfig(batch.getCodeConfigJson())
                .durationUnit(batch.getDurationUnit())
                .durationValue(batch.getDurationValue())
                .generatedCount(batch.getGeneratedCount())
                .redeemedCount(batch.getRedeemedCount())
                .distributionChannels(batch.getDistributionChannelsJson())
                .status(batch.getStatus())
                .build();
    }
}
