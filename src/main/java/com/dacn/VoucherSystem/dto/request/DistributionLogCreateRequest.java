package com.dacn.VoucherSystem.dto.request;

import com.dacn.VoucherSystem.service.interfaces.DistributionLogService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record DistributionLogCreateRequest(
        @NotNull
        Long batchId,

        @NotNull
        Long campaignId,

        @NotBlank
        @Pattern(
                regexp = DistributionLogService.ALLOWED_CHANNELS,
                flags = Pattern.Flag.CASE_INSENSITIVE,
                message = "channel must be EMAIL or WEBHOOK"
        )
        String channel,

        @NotBlank
        @Size(max = 255, message = "targetAddress must be at most 255 characters")
        String targetAddress
) {
}