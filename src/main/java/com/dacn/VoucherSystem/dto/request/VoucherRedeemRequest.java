package com.dacn.VoucherSystem.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

public record VoucherRedeemRequest (
    @NotBlank
    @Size(max = 50)
    String voucherCode,

    @NotBlank
    @Size(max = 50)
    String customerId,

    @NotNull
    @DecimalMin("0.00")
    BigDecimal cartSubtotal,

    Set<String> customerSegments,

    @NotNull
    List<@Valid CartItemRequest> items,

    @NotBlank
    @Size(max = 45)
    String ipAddress,

    @NotBlank
    @Size(max = 255)
    String deviceFingerprint
){

    public record CartItemRequest(
            @NotBlank String productId,
            @Positive int quantity,
            @NotNull @DecimalMin("0.00") BigDecimal unitPrice
    ) {}
}
