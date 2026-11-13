package com.dacn.VoucherSystem.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record DashboardOverviewResponse(
        OverviewSummary summary,
        List<CampaignOverview> campaigns,
        List<CodeOverview> codes
) {
    public record OverviewSummary(
            long totalIssued,
            long totalRedeemed,
            double redemptionRate,
            long totalDistributed,
            // đã dùng / đã phân phối (%)
            double distributedRedemptionRate,
            BigDecimal totalOriginalAmount,
            BigDecimal totalDiscountAmount,
            BigDecimal totalFinalAmount,
            double roi
    ) {
    }

    public record CampaignOverview(
            String campaignId,
            String campaignName,
            long issuedCount,
            long redeemedCount,
            double redemptionRate,
            long distributedCount,
            double distributedRedemptionRate,
            BigDecimal totalOriginalAmount,
            BigDecimal totalDiscountAmount,
            BigDecimal totalFinalAmount,
            double roi
    ) {
    }

    public record CodeOverview(
            String campaignId,
            String batchId,
            String code,
            long issuedCount,
            long redeemedCount,
            double redemptionRate,
            long distributedCount,
            double distributedRedemptionRate,
            BigDecimal totalOriginalAmount,
            BigDecimal totalDiscountAmount,
            BigDecimal totalFinalAmount,
            double roi
    ) {
    }
}