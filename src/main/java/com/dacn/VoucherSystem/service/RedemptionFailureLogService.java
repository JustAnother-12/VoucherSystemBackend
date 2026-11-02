package com.dacn.VoucherSystem.service;

import com.dacn.VoucherSystem.cache.VoucherRuleSnapshot;
import com.dacn.VoucherSystem.engine.result.RewardResult;
import com.dacn.VoucherSystem.entity.RedemptionLog;
import com.dacn.VoucherSystem.enums.RedemptionStatus;
import com.dacn.VoucherSystem.repository.RedemptionLogRepository;
import com.dacn.VoucherSystem.repository.VoucherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class RedemptionFailureLogService {

    private final RedemptionLogRepository redemptionLogRepository;
    private final VoucherRepository voucherRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveFailed(
            Long voucherId,
            String customerId,
            BigDecimal cartSubtotal,
            String ipAddress,
            String deviceFingerprint,
            String reason
    ) {
        BigDecimal original = cartSubtotal.setScale(2, RoundingMode.HALF_UP);

        String safeReason = reason.length() > 255
                ? reason.substring(0, 255)
                : reason;

        RedemptionLog log = RedemptionLog.builder()
                .voucher(voucherRepository.getReferenceById(voucherId))
                .customerId(customerId)
                .ipAddress(ipAddress)
                .deviceFingerprint(deviceFingerprint)
                .originalAmount(original)
                .discountAmount(new BigDecimal("0.00"))
                .finalAmount(original)
                .status(RedemptionStatus.FAILED)
                .failureReason(safeReason)
                .build();

        redemptionLogRepository.save(log);
    }
}
