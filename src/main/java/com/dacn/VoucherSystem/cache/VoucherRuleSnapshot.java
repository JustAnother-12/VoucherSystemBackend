package com.dacn.VoucherSystem.cache;

import com.dacn.VoucherSystem.enums.*;
import tools.jackson.databind.JsonNode;

import java.time.LocalDateTime;

public record VoucherRuleSnapshot(
        Long voucherId,
        String code,
        Long batchId,
        BatchType batchType,
        BatchStatus batchStatus,
        int generatedCount,
        int maxUsageCount,
        Long campaignId,
        CampaignStatus campaignStatus,
        LocalDateTime campaignStartTime,
        LocalDateTime campaignEndTime,
        ConditionType conditionType,
        RuleStatus conditionStatus,
        JsonNode conditionJson,
        RewardType rewardType,
        RuleStatus rewardStatus,
        JsonNode rewardJson
) {}
