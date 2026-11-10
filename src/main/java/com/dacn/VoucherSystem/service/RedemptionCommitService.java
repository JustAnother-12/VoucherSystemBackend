package com.dacn.VoucherSystem.service;

import com.dacn.VoucherSystem.cache.VoucherRuleSnapshot;
import com.dacn.VoucherSystem.dto.response.VoucherRedeemResponse;
import com.dacn.VoucherSystem.engine.result.RewardResult;
import com.dacn.VoucherSystem.entity.RedemptionLog;
import com.dacn.VoucherSystem.enums.BatchType;
import com.dacn.VoucherSystem.enums.CustomerVoucherStatus;
import com.dacn.VoucherSystem.enums.RedemptionStatus;
import com.dacn.VoucherSystem.exception.RedemptionConflictException;
import com.dacn.VoucherSystem.repository.CustomerVoucherRepository;
import com.dacn.VoucherSystem.repository.RedemptionLogRepository;
import com.dacn.VoucherSystem.repository.VoucherBatchRepository;
import com.dacn.VoucherSystem.repository.VoucherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class RedemptionCommitService {

    private final VoucherBatchRepository voucherBatchRepository;
    private final CustomerVoucherRepository customerVoucherRepository;
    private final RedemptionLogRepository redemptionLogRepository;
    private final VoucherRepository voucherRepository;
    private final Clock clock;

    @Transactional
    public VoucherRedeemResponse commit(
            VoucherRuleSnapshot snapshot,
            String customerId,
            BigDecimal cartSubtotal,
            String ipAddress,
            String deviceFingerprint,
            RewardResult reward
    ) {
        BigDecimal original = cartSubtotal.setScale(2, RoundingMode.HALF_UP);

        BigDecimal discount = reward.discountAmount()
                .setScale(2, RoundingMode.HALF_UP);

        if (discount.signum() < 0 || discount.compareTo(original) > 0) {
            throw new IllegalStateException("Invalid discount amount");
        }

        LocalDateTime commitTime = LocalDateTime.now(clock);

        // BULK vouchers must be assigned to this customer and AVAILABLE.
        if (snapshot.batchType() == BatchType.BULK) {
            int consumed = customerVoucherRepository.consumeAvailable(
                    snapshot.voucherId(),
                    customerId,
                    CustomerVoucherStatus.AVAILABLE,
                    CustomerVoucherStatus.USED,
                    commitTime
            );

            if (consumed != 1) {
                // Runtime exception makes the quota increment roll back too.
                throw new RedemptionConflictException(
                        "Voucher is no longer available to this customer"
                );
            }
        }

        RedemptionLog log = RedemptionLog.builder()
                .createdAt(LocalDateTime.now(clock))
                .voucher(voucherRepository.getReferenceById(
                        snapshot.voucherId()
                ))
                .customerId(customerId)
                .ipAddress(ipAddress)
                .deviceFingerprint(deviceFingerprint)
                .originalAmount(original)
                .discountAmount(discount)
                .finalAmount(original.subtract(discount))
                .status(RedemptionStatus.SUCCESS)
                .failureReason("")
                .build();

        RedemptionLog saved = redemptionLogRepository.saveAndFlush(log);

        VoucherRedeemResponse response = new VoucherRedeemResponse(
                saved.getId(),
                snapshot.voucherId(),
                snapshot.code(),
                original,
                discount,
                original.subtract(discount),
                reward.lineDiscounts()
        );

        // Claim quota
        int quotaClaimed = voucherBatchRepository.tryConsumeRedemptionQuota(
                snapshot.batchId(),
                snapshot.voucherId(),
                commitTime
        );

        if (quotaClaimed != 1) {
            throw new RedemptionConflictException(
                    "Batch quota or campaign availability has changed"
            );
        }

        return response;
    }
}
