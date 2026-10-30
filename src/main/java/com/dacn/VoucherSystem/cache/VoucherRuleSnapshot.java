package com.dacn.VoucherSystem.cache;

import com.dacn.VoucherSystem.enums.*;

import java.time.LocalDateTime;

public record VoucherRuleSnapshot(
        Long voucherId,
        String code,
        Long batchId,
        BatchType batchType,
        BatchStatus batchStatus,
        int generatedCount,
        Long campaignId,
        CampaignStatus campaignStatus,
        LocalDateTime campaignStartTime,
        LocalDateTime campaignEndTime,
        ConditionType conditionType,
        RuleStatus conditionStatus,
        String conditionJson,
        RewardType rewardType,
        RuleStatus rewardStatus,
        String rewardJson
) {}
