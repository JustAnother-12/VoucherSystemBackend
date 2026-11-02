package com.dacn.VoucherSystem.dto.request;

import com.dacn.VoucherSystem.enums.BlacklistType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record BlackListCreateRequest(

        @NotNull
        BlacklistType blacklistType,

        @NotBlank
        @Size(max = 255)
        String targetValue,

        @NotBlank
        @Size(max = 255)
        String reason,

        LocalDateTime expiredAt
) {
}
