package com.dacn.VoucherSystem.dto.request;

import com.dacn.VoucherSystem.enums.ActionOnExceed;
import com.dacn.VoucherSystem.enums.LimitScope;
import lombok.Data;
import org.openapitools.jackson.nullable.JsonNullable;

public record RateLimitConfigUpdateRequest(
        JsonNullable<String> name,

        JsonNullable<String> endpointPattern,

        JsonNullable<LimitScope> limitScope,

        JsonNullable<Integer> maxRequest,

        JsonNullable<Integer> timeWindowSeconds,

        JsonNullable<ActionOnExceed> actionOnExceed,

        JsonNullable<Integer> priority,

        JsonNullable<Boolean> isActive
) {
}
