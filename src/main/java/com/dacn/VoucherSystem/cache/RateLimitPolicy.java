package com.dacn.VoucherSystem.cache;

import com.dacn.VoucherSystem.enums.ActionOnExceed;
import com.dacn.VoucherSystem.enums.LimitScope;

public record RateLimitPolicy(
        Long id,
        String endpointPattern,
        LimitScope limitScope,
        Integer maxRequest,
        Integer timeWindowSeconds,
        ActionOnExceed actionOnExceed,
        Integer priority
) {
}
