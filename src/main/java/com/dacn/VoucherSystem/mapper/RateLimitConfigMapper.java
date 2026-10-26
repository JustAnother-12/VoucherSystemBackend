package com.dacn.VoucherSystem.mapper;

import com.dacn.VoucherSystem.dto.response.RateLimitConfigResponse;
import com.dacn.VoucherSystem.entity.RateLimitConfig;
import org.springframework.stereotype.Component;

@Component
public class RateLimitConfigMapper {
    public RateLimitConfigResponse toRateLimitConfigResponse(RateLimitConfig rateLimitConfig) {
        if (rateLimitConfig == null) {
            return null;
        }
        return RateLimitConfigResponse.builder()
                .id(rateLimitConfig.getId())
                .name(rateLimitConfig.getRateLimitConfigName())
                .scope(rateLimitConfig.getLimitScope())
                .maxRequest(rateLimitConfig.getMaxRequest())
                .timeWindowSeconds(rateLimitConfig.getTimeWindowSeconds())
                .actionOnExceed(rateLimitConfig.getActionOnExceed())
                .priority(rateLimitConfig.getPriority())
                .endpointPattern(rateLimitConfig.getEndpointPattern())
                .isActive(rateLimitConfig.getIsActive())
                .createAt(rateLimitConfig.getCreatedAt())
                .build();
    }
}
