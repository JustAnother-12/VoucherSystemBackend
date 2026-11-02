package com.dacn.VoucherSystem.engine.result;

import java.math.BigDecimal;

public record LineDiscount(String productId, int quantity, BigDecimal discountAmount) {}
