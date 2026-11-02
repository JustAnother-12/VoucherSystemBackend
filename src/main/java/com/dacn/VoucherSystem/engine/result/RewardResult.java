package com.dacn.VoucherSystem.engine.result;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

public record RewardResult(BigDecimal discountAmount, List<LineDiscount> lineDiscounts) {
    public RewardResult {
        Objects.requireNonNull(discountAmount, "discountAmount is required");
        if (discountAmount.signum() < 0) throw new IllegalArgumentException("discountAmount must be >= 0");
        lineDiscounts = lineDiscounts == null ? List.of() : List.copyOf(lineDiscounts);
    }

    public static RewardResult cartDiscount(BigDecimal amount) {
        return new RewardResult(amount, List.of());
    }
}
