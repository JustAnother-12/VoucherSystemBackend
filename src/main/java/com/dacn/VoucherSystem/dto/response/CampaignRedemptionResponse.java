package com.dacn.VoucherSystem.dto.response;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Builder;

import java.util.List;

@Builder
@JsonPropertyOrder({
        "campaignId",
        "campaignName",
        "campaignSummary",
        "batches"
})
public record CampaignRedemptionResponse(
        String campaignId,
        String campaignName,
        CampaignSummary campaignSummary,
        List<BatchRedemptionResponse> batches
) {
}
