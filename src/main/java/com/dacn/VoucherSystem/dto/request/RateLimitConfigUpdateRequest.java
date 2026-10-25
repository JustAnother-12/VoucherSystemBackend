package com.dacn.VoucherSystem.dto.request;

import com.dacn.VoucherSystem.enums.ActionOnExceed;
import com.dacn.VoucherSystem.enums.LimitScope;
import lombok.Data;
import org.openapitools.jackson.nullable.JsonNullable;

@Data
public class RateLimitConfigUpdateRequest {
    private JsonNullable<String> name = JsonNullable.undefined();

    private JsonNullable<String> endpointPattern = JsonNullable.undefined();

    private JsonNullable<LimitScope> limitScope = JsonNullable.undefined();

    private JsonNullable<Integer> maxRequest = JsonNullable.undefined();

    private JsonNullable<Integer> timeWindowSeconds = JsonNullable.undefined();

    private JsonNullable<ActionOnExceed> actionOnExceed = JsonNullable.undefined();

    private JsonNullable<Integer> priority = JsonNullable.undefined();

    private JsonNullable<Boolean> isActive = JsonNullable.undefined();
}
