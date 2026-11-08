package com.dacn.VoucherSystem.dto.response;

import com.dacn.VoucherSystem.enums.CustomerVoucherStatus;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
@JsonPropertyOrder({
        "id",
        "customerId",
        "voucherId",
        "voucherCode",
        "status",
        "claimedAt",
        "expiredAt",
})
public record CustomerVoucherResponse(
        Long id,
        String customerId,
        Long voucherId,
        String voucherCode,
        CustomerVoucherStatus status,
        LocalDateTime claimedAt,
        LocalDateTime expiredAt
) {}