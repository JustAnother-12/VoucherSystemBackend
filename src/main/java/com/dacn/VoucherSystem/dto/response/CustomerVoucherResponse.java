package com.dacn.VoucherSystem.dto.response;

import com.dacn.VoucherSystem.enums.CustomerVoucherStatus;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record CustomerVoucherResponse(
        Long id,
        String customerId,
        Long voucherId,
        String voucherCode,
        CustomerVoucherStatus status,
        LocalDateTime claimedAt,
        LocalDateTime expiredAt
) {}