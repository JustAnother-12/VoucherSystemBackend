package com.dacn.VoucherSystem.util;

import com.dacn.VoucherSystem.enums.DurationUnit;

import java.time.LocalDateTime;

public final class CustomerVoucherUtil {
    private CustomerVoucherUtil(){}

    public static LocalDateTime calculateExpiredAt(LocalDateTime from, DurationUnit unit, Integer value) {
        return switch (unit) {
            case SECONDS -> from.plusSeconds(value);
            case MINUTES -> from.plusMinutes(value);
            case HOURS -> from.plusHours(value);
            case DAYS -> from.plusDays(value);
        };
    }
}
