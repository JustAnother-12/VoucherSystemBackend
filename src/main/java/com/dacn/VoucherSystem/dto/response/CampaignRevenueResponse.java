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
public record CampaignRevenueResponse(
        String campaignId,
        String campaignName,
        CampaignRevenueSummary campaignSummary,
        List<BatchRevenueResponse> batches
) {
}
