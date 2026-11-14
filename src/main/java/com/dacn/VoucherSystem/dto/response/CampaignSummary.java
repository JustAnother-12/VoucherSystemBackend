package com.dacn.VoucherSystem.dto.response;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Builder;

@Builder
@JsonPropertyOrder({
        "totalIssued",
        "totalRedeemed",
        "redemptionRate",
        "totalDistributed",
        "distributedRedemptionRate"
})
public record CampaignSummary(
        long totalIssued,
        long totalRedeemed,
        double redemptionRate,
        long totalDistributed,
        double distributedRedemptionRate
) {
}
