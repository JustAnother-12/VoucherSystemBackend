package com.dacn.VoucherSystem.service;

import com.dacn.VoucherSystem.dto.response.RateLimitConfigResponse;
import com.dacn.VoucherSystem.enums.ActionOnExceed;
import com.dacn.VoucherSystem.enums.LimitScope;
import jakarta.annotation.Nullable;
import org.openapitools.jackson.nullable.JsonNullable;

import java.util.List;

public interface RateLimitConfigService {
    List<RateLimitConfigResponse> getAllRateLimitConfig();
    RateLimitConfigResponse getRateLimitConfigById(Long id);
    RateLimitConfigResponse createRateLimitConfig(
            String name,
            String endpointPattern,
            LimitScope scope,
            Integer maxRequest,
            Integer timeWindowSeconds,
            ActionOnExceed actionOnExceed,
            Integer priority
    );
    RateLimitConfigResponse updateRateLimitConfig(
            Long id,
            JsonNullable<String> name,
            JsonNullable<String> endpointPattern,
            JsonNullable<LimitScope> limitScope,
            JsonNullable<Integer> maxRequest,
            JsonNullable<Integer> timeWindowSeconds,
            JsonNullable<ActionOnExceed> actionOnExceed,
            JsonNullable<Integer> priority,
            JsonNullable<Boolean> isActive
    );
}
