package com.dacn.VoucherSystem.realtime;

import java.math.BigDecimal;
import java.time.Instant;

public record DashboardEvent(
        String type,
        Long voucherId,
        String code,
        BigDecimal discountAmount,
        BigDecimal finalAmount,
        Instant occurredAt
) {}