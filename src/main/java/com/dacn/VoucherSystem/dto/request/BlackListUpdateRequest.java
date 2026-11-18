package com.dacn.VoucherSystem.dto.request;

import com.dacn.VoucherSystem.enums.BlacklistType;
import org.openapitools.jackson.nullable.JsonNullable;

import java.time.LocalDateTime;

public record BlackListUpdateRequest(
        JsonNullable<BlacklistType> blacklistType,
        JsonNullable<String> targetValue,
        JsonNullable<String> reason,
        JsonNullable<LocalDateTime> expiredAt,
        JsonNullable<Boolean> isActive
) {
}