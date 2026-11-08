package com.dacn.VoucherSystem.dto.response;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Builder;

@Builder
@JsonPropertyOrder({
        "totalIssued",
        "totalRedeemed",
        "redemptionRate"
})
public record CampaignSummary(
        long totalIssued,
        long totalRedeemed,
        double redemptionRate
) {
}
