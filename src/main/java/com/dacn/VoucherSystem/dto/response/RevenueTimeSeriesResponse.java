package com.dacn.VoucherSystem.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record RevenueTimeSeriesResponse(
        String granularity,
        LocalDateTime from,
        LocalDateTime to,
        String campaignId,
        List<Point> points
) {
    public record Point(
            LocalDateTime bucket,
            long redemptionCount,
            BigDecimal originalAmount,
            BigDecimal discountAmount,
            BigDecimal finalAmount
    ) {
    }
}