package com.dacn.VoucherSystem.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class StatisticUtil {

    private StatisticUtil() {
    }

    public static double calculateRate(long redeemed, long denominator) {
        if (denominator <= 0) {
            return 0.0;
        }
        return BigDecimal.valueOf(redeemed)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(denominator), 2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    public static double calculateRoi(BigDecimal incrementalRevenue, BigDecimal discountAmount) {
        if (incrementalRevenue == null || discountAmount == null || discountAmount.signum() <= 0) {
            return 0.0;
        }
        return incrementalRevenue.subtract(discountAmount)
                .multiply(BigDecimal.valueOf(100))
                .divide(discountAmount, 2, RoundingMode.HALF_UP)
                .doubleValue();
    }
}