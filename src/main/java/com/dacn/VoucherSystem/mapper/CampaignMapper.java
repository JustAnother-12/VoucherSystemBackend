package com.dacn.VoucherSystem.mapper;

import com.dacn.VoucherSystem.dto.response.CampaignResponse;
import com.dacn.VoucherSystem.entity.Campaign;
import com.dacn.VoucherSystem.entity.VoucherBatch;
import org.springframework.stereotype.Component;

@Component
public class CampaignMapper {
    public CampaignResponse toCampaignResponse(Campaign campaign) {
        if (campaign == null) {
            return null;
        }
        return CampaignResponse.builder()
                .id(campaign.getId())
                .name(campaign.getCampaignName())
                .startTime(campaign.getStartTime())
                .endTime(campaign.getEndTime())
                .status(campaign.getStatus())
                .batchIds(campaign.getVoucherBatches().stream().map(VoucherBatch::getId).toList())
                .adminId(campaign.getAdmin().getId())
                .build();
    }
}
