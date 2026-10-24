package com.dacn.VoucherSystem.dto.response;

import com.dacn.VoucherSystem.enums.ActionOnExceed;
import com.dacn.VoucherSystem.enums.LimitScope;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class RateLimitConfigResponse {
    private Long id;
    private String name;
    private String endpointPattern;
    private LimitScope scope;
    private Integer maxRequest;
    private Integer timeWindowSeconds;
    private ActionOnExceed actionOnExceed;
    private Integer priority;
    private Boolean isActive;
}
