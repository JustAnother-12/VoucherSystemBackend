package com.dacn.VoucherSystem.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class StatisticUtil {

    private StatisticUtil() {
    }

    public static double calculateRate(long redeemed, long issued) {
        if (issued <= 0) {
            return 0.0;
        }
        return BigDecimal.valueOf(redeemed)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(issued), 2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    public static double calculateRoi(BigDecimal totalFinalAmount, BigDecimal totalDiscountAmount) {
        if (totalDiscountAmount == null || totalDiscountAmount.compareTo(BigDecimal.ZERO) <= 0
                || totalFinalAmount == null) {
            return 0.0;
        }
        return totalFinalAmount
                .multiply(BigDecimal.valueOf(100))
                .divide(totalDiscountAmount, 2, RoundingMode.HALF_UP)
                .doubleValue();
    }
}
