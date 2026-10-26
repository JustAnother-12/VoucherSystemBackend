package com.dacn.VoucherSystem.dto.request;

import com.dacn.VoucherSystem.enums.ActionOnExceed;
import com.dacn.VoucherSystem.enums.LimitScope;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RateLimitConfigCreateRequest {
    @NotBlank(message = "name must not be empty")
    private String name;

    @NotBlank(message = "endpoint pattern must not be empty")
    private String endpointPattern;

    @NotNull
    private LimitScope limitScope;

    @NotNull
    private Integer maxRequest;

    @NotNull
    private Integer timeWindowSeconds;

    @NotNull
    private ActionOnExceed actionOnExceed;

    @NotNull
    private Integer priority;
}
