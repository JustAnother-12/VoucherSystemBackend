package com.dacn.VoucherSystem.dto.response;

import com.dacn.VoucherSystem.enums.ActionOnExceed;
import com.dacn.VoucherSystem.enums.LimitScope;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Builder
@JsonPropertyOrder({
        "id",
        "name",
        "endpointPattern",
        "scope",
        "maxRequest",
        "timeWindowSeconds",
        "actionOnExceed",
        "priority",
        "isActive",
        "createAt"
})
public record RateLimitConfigResponse(
        Long id,
        String name,
        String endpointPattern,
        LimitScope scope,
        Integer maxRequest,
        Integer timeWindowSeconds,
        ActionOnExceed actionOnExceed,
        Integer priority,
        Boolean isActive,
        LocalDateTime createAt
) {
}
