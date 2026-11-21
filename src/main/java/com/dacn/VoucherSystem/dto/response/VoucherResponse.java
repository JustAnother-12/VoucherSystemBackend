package com.dacn.VoucherSystem.dto.response;

import com.dacn.VoucherSystem.enums.BatchStatus;
import com.dacn.VoucherSystem.enums.BatchType;
import com.dacn.VoucherSystem.enums.CampaignStatus;
import com.dacn.VoucherSystem.enums.ConditionType;
import com.dacn.VoucherSystem.enums.RewardType;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Builder;
import tools.jackson.databind.JsonNode;

import java.time.LocalDateTime;

@Builder
@JsonPropertyOrder({
        "id",
        "code",
        "createdAt",
        "campaign",
        "batch",
        "reward",
        "condition"
})
public record VoucherResponse(
        Long id,
        String code,
        LocalDateTime createdAt,
        CampaignInfo campaign,
        BatchInfo batch,
        RewardInfo reward,
        ConditionInfo condition
) {
    public record CampaignInfo(
            Long id,
            String name,
            CampaignStatus status,
            LocalDateTime startTime,
            LocalDateTime endTime
    ) {
    }

    public record BatchInfo(
            Long id,
            BatchType batchType,
            BatchStatus status,
            Integer generatedCount,
            Integer maxUsageCount,
            Long redeemedCount
    ) {
    }

    public record RewardInfo(String name, RewardType type, JsonNode config) {
    }

    public record ConditionInfo(String name, ConditionType type, JsonNode config) {
    }
}