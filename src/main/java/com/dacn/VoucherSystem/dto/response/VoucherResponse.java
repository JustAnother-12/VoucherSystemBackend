package com.dacn.VoucherSystem.dto.response;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
@JsonPropertyOrder({
        "id",
        "code",
        "batchId",
        "campaignId",
        "createdAt",
})
public record VoucherResponse(
        Long id,
        String code,
        Long batchId,
        Long campaignId,
        LocalDateTime createdAt
) {}