package com.dacn.VoucherSystem.dto.request;

import com.dacn.VoucherSystem.enums.ActionOnExceed;
import com.dacn.VoucherSystem.enums.LimitScope;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

public record RateLimitConfigCreateRequest(
        @NotBlank(message = "name must not be empty")
        String name,

        @NotBlank(message = "endpoint pattern must not be empty")
        String endpointPattern,

        @NotNull
        LimitScope limitScope,

        @NotNull
        @Positive
        Integer maxRequest,

        @NotNull
        @Positive
        Integer timeWindowSeconds,

        @NotNull
        ActionOnExceed actionOnExceed,

        @NotNull
        Integer priority
) {
}
