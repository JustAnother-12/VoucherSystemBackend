package com.dacn.VoucherSystem.event;

import com.dacn.VoucherSystem.enums.BlacklistType;

import java.time.LocalDateTime;

public record BlackListChangedEvent(
        BlacklistType blacklistType,
        String targetValue,
        boolean active,
        LocalDateTime expiredAt
) {
}
