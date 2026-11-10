package com.dacn.VoucherSystem.service;

import com.dacn.VoucherSystem.cache.BlackListStatusCache;
import com.dacn.VoucherSystem.enums.BlacklistType;
import com.dacn.VoucherSystem.exception.RedemptionBlockedException;
import com.dacn.VoucherSystem.service.interfaces.FraudLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RedemptionFraudGuard {

    private final BlackListStatusCache blacklistCache;
    private final FraudLogService fraudLogService;

    public void verifyAllowed(
            Long voucherId,
            String customerId,
            String ipAddress,
            String deviceFingerprint
    ) {
        Map<BlacklistType, String> subjects = new EnumMap<>(BlacklistType.class);

        subjects.put(BlacklistType.CUSTOMER, customerId);
        subjects.put(BlacklistType.IP, ipAddress);
        subjects.put(
                BlacklistType.DEVICE_FINGERPRINT,
                deviceFingerprint
        );

        Set<BlacklistType> blockedTypes = blacklistCache.findBlockedTypes(subjects);

        if (blockedTypes.isEmpty()) {
            return;
        }

        for (BlacklistType type : blockedTypes) {
            fraudLogService.recordAutomatedFraud(
                    voucherId,
                    customerId,
                    ipAddress,
                    deviceFingerprint,
                    "BLACKLIST_" + type.name()
            );
        }

        throw new RedemptionBlockedException(
                "This customer or device is blocked from redeeming vouchers"
        );
    }
}
