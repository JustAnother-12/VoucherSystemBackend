package com.dacn.VoucherSystem.dto.request;

import com.dacn.VoucherSystem.enums.CustomerVoucherStatus;
import jakarta.validation.constraints.NotNull;

public record CustomerVoucherUpdateRequest(
        @NotNull
        CustomerVoucherStatus status
) {}