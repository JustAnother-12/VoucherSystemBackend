package com.dacn.VoucherSystem.dto.response;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record VoucherResponse(
        Long id,
        String code,
        Long batchId,
        Long campaignId,
        LocalDateTime createdAt
) {}