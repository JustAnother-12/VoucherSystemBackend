package com.dacn.VoucherSystem.dto.response;

import com.dacn.VoucherSystem.enums.DistributionStatus;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Builder
@JsonPropertyOrder({
        "id",
        "voucherId",
        "campaignId",
        "channel",
        "targetAddress",
        "status",
        "sentAt"
})
public record DistributionLogResponse(
        Long id,
        Long voucherId,
        Long campaignId,
        String channel,
        String targetAddress,
        DistributionStatus status,
        LocalDateTime sentAt
) {
}
