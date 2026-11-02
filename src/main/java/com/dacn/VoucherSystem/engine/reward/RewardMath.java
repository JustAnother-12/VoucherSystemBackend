package com.dacn.VoucherSystem.engine.reward;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class RewardMath {
    public static final BigDecimal HUNDRED = new BigDecimal("100");
    private RewardMath() {}

    public static BigDecimal money(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP);
    }
    public static BigDecimal percentage(BigDecimal base, BigDecimal percent) {
        return money(base.multiply(percent).divide(HUNDRED, 8, RoundingMode.HALF_UP));
    }
    public static BigDecimal min(BigDecimal a, BigDecimal b) {
        return a.compareTo(b) <= 0 ? a : b;
    }
    public static BigDecimal nonNegative(BigDecimal value) {
        return value.signum() < 0 ? BigDecimal.ZERO.setScale(2) : money(value);
    }
}
