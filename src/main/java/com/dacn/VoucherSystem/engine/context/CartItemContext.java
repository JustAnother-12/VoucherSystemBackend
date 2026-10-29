package com.dacn.VoucherSystem.engine.context;

import java.math.BigDecimal;
import java.util.Objects;

public record CartItemContext(
        String productId,
        int quantity,
        BigDecimal unitPrice
) {
    public CartItemContext {
        if (productId == null || productId.isBlank()) throw new IllegalArgumentException("productId is required");
        if (quantity <= 0) throw new IllegalArgumentException("quantity must be positive");
        Objects.requireNonNull(unitPrice, "unitPrice is required");
        if (unitPrice.signum() < 0) throw new IllegalArgumentException("unitPrice must be >= 0");
    }
}
