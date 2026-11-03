package com.dacn.VoucherSystem.dto.response;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
@JsonPropertyOrder({
        "totalOriginalAmount",
        "totalDiscountAmount",
        "totalFinalAmount",
        "roi"
})
public record CampaignRevenueSummary(
        BigDecimal totalOriginalAmount,
        BigDecimal totalDiscountAmount,
        BigDecimal totalFinalAmount,
        double roi
) {
}