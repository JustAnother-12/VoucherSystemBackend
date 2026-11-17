package com.dacn.VoucherSystem.mapper;

import com.dacn.VoucherSystem.dto.response.VoucherResponse;
import com.dacn.VoucherSystem.entity.BatchRule;
import com.dacn.VoucherSystem.entity.Campaign;
import com.dacn.VoucherSystem.entity.Voucher;
import com.dacn.VoucherSystem.entity.VoucherBatch;
import org.springframework.stereotype.Component;

@Component
public class VoucherMapper {
    public VoucherResponse toVoucherResponse(Voucher voucher) {
        VoucherBatch batch = voucher.getBatch();
        Campaign campaign = batch.getCampaign();
        BatchRule rule = batch.getRule();

        return VoucherResponse.builder()
                .id(voucher.getId())
                .code(voucher.getCode())
                .createdAt(voucher.getCreatedAt())
                .campaign(new VoucherResponse.CampaignInfo(
                        campaign.getId(),
                        campaign.getCampaignName(),
                        campaign.getStatus(),
                        campaign.getStartTime(),
                        campaign.getEndTime()))
                .batch(new VoucherResponse.BatchInfo(
                        batch.getId(),
                        batch.getBatchType(),
                        batch.getStatus(),
                        batch.getGeneratedCount(),
                        batch.getMaxUsageCount(),
                        batch.getRedeemedCount()))
                .reward(new VoucherResponse.RewardInfo(
                        rule.getReward().getRewardName(),
                        rule.getReward().getRewardType(),
                        rule.getReward().getRewardJson()))
                .condition(new VoucherResponse.ConditionInfo(
                        rule.getCondition().getConditionName(),
                        rule.getCondition().getConditionType(),
                        rule.getCondition().getConditionJson()))
                .build();
    }
}