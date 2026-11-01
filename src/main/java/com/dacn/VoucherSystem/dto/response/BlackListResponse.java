package com.dacn.VoucherSystem.dto.response;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Builder
@JsonPropertyOrder({
        "id",
        "type",
        "value",
        "reason",
        "isActive",
        "expiredAt",
        "createAt"
})
public record BlackListResponse(
        Long id,
        String type,
        String value,
        String reason,
        Boolean isActive,
        LocalDateTime expiredAt,
        LocalDateTime createAt
) {
}
