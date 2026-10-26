package com.dacn.VoucherSystem.dto.response;

import com.dacn.VoucherSystem.enums.ActionOnExceed;
import com.dacn.VoucherSystem.enums.LimitScope;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
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
    private LocalDateTime createAt;
}
