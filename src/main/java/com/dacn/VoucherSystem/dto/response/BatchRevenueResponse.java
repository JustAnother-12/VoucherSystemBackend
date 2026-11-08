package com.dacn.VoucherSystem.dto.response;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;

@Builder
@JsonPropertyOrder({
        "batchId",
        "totalOriginalAmount",
        "totalDiscountAmount",
        "totalFinalAmount",
        "roi",
        "codes"
})
public record BatchRevenueResponse(
        String batchId,
        BigDecimal totalOriginalAmount,
        BigDecimal totalDiscountAmount,
        BigDecimal totalFinalAmount,
        double roi,
        List<CodeRevenueResponse> codes
) {
}
