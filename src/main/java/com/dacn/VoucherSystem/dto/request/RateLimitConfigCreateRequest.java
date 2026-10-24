package com.dacn.VoucherSystem.dto.request;

import com.dacn.VoucherSystem.enums.ActionOnExceed;
import com.dacn.VoucherSystem.enums.LimitScope;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RateLimitConfigCreateRequest {
    @NotBlank(message = "name must not be empty")
    private String name;

    @NotBlank(message = "endpoint pattern must not be empty")
    private String endpointPattern;

    @NotBlank(message = "scope must not be empty")
    private LimitScope limitScope;

    @NotBlank(message = "max request must not be empty")
    private Integer maxRequest;

    @NotBlank(message = "time window must not be empty")
    private Integer timeWindowSeconds;

    @NotBlank(message = "action on exceed must not be empty")
    private ActionOnExceed actionOnExceed;

    @NotBlank(message = "priority must not be empty")
    private Integer priority;
}
