package com.dacn.VoucherSystem.dto.request;

import com.dacn.VoucherSystem.enums.CampaignStatus;
import org.openapitools.jackson.nullable.JsonNullable;

import java.time.LocalDateTime;

public record CampaignUpdateRequest(
        JsonNullable<String> name,
        JsonNullable<LocalDateTime> startTime,
        JsonNullable<LocalDateTime> endTime,
        JsonNullable<CampaignStatus> status
) {
}
