package com.dacn.VoucherSystem.dto.response;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
@JsonPropertyOrder({
        "code",
        "totalOriginalAmount",
        "totalDiscountAmount",
        "totalFinalAmount",
        "roi"
})
public record CodeRevenueResponse(
        String code,
        BigDecimal totalOriginalAmount,
        BigDecimal totalDiscountAmount,
        BigDecimal totalFinalAmount,
        double roi
) {
}
