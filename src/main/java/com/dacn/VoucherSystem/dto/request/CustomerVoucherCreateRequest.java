package com.dacn.VoucherSystem.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CustomerVoucherCreateRequest(

        @NotBlank
        @Size(max = 50)
        String customerId,

        @NotNull
        Long voucherId
) {}