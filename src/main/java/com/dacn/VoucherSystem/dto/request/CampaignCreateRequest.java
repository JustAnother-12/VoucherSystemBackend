package com.dacn.VoucherSystem.dto.request;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record CampaignCreateRequest(
        @NotNull String name,
        @NotNull LocalDateTime startTime,
        @NotNull LocalDateTime endTime
        ) {
}
