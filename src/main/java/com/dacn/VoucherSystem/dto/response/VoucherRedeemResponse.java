package com.dacn.VoucherSystem.dto.response;

import com.dacn.VoucherSystem.engine.result.LineDiscount;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.math.BigDecimal;
import java.util.List;

@JsonPropertyOrder({
        "redemptionLogId",
        "voucherId",
        "code",
        "originalAmount",
        "discountAmount",
        "finalAmount",
        "lineDiscounts",
})
public record VoucherRedeemResponse (
        Long redemptionLogId,
        Long voucherId,
        String code,
        BigDecimal originalAmount,
        BigDecimal discountAmount,
        BigDecimal finalAmount,
        List<LineDiscount> lineDiscounts
) {}
