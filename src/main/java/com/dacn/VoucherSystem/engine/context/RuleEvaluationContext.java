package com.dacn.VoucherSystem.engine.context;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public record RuleEvaluationContext(
        BigDecimal cartSubtotal,
        Set<String> customerSegments,
        Instant evaluatedAt,
        List<CartItemContext> items
) {
    public RuleEvaluationContext {
        Objects.requireNonNull(cartSubtotal, "cartSubtotal is required");
        Objects.requireNonNull(evaluatedAt, "evaluatedAt is required");
        if (cartSubtotal.signum() < 0) throw new IllegalArgumentException("cartSubtotal must be >= 0");
        customerSegments = customerSegments == null ? Set.of() : Set.copyOf(customerSegments);
        items = items == null ? List.of() : List.copyOf(items);
    }
}
