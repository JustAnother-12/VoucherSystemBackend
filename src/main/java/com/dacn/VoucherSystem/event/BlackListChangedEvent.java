package com.dacn.VoucherSystem.event;

import com.dacn.VoucherSystem.enums.BlacklistType;

public record BlackListChangedEvent(
        BlacklistType blacklistType,
        String targetValue
) {
}
