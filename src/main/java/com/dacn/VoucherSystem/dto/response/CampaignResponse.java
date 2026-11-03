package com.dacn.VoucherSystem.dto.response;

import com.dacn.VoucherSystem.enums.CampaignStatus;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

@Builder
@JsonPropertyOrder({
        "id",
        "name",
        "startTime",
        "endTime",
        "status",
        "batchIds",
        "adminId"
})
public record CampaignResponse(
        Long id,
        String name,
        LocalDateTime startTime,
        LocalDateTime endTime,
        CampaignStatus status,
        List<Long> batchIds,
        Long adminId
) {
}
