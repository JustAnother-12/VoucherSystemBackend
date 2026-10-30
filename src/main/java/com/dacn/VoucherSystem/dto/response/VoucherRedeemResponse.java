package com.dacn.VoucherSystem.dto.response;

import com.dacn.VoucherSystem.engine.result.LineDiscount;

import java.math.BigDecimal;
import java.util.List;

public record VoucherRedeemResponse (
        Long redemptionLogId,
        Long voucherId,
        String code,
        BigDecimal originalAmount,
        BigDecimal discountAmount,
        BigDecimal finalAmount,
        List<LineDiscount> lineDiscounts
) {}
