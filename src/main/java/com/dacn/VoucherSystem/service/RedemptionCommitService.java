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
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class RedemptionCommitService {
    private static final int MAX_COUNTER_SHARDS = 32;

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
        boolean quotaClaimed = tryConsumeQuota(
                snapshot,
                commitTime
        );

        if (!quotaClaimed) {
            throw new RedemptionConflictException(
                    "Batch quota or campaign availability has changed"
            );
        }

        return response;
    }

    private boolean tryConsumeQuota(
            VoucherRuleSnapshot snapshot,
            LocalDateTime now
    ) {
        int maxUsageCount = snapshot.maxUsageCount();

        if (maxUsageCount <= 0) {
            return false;
        }

        int shardCount = Math.min(
                MAX_COUNTER_SHARDS,
                maxUsageCount
        );

        // Random starting point to distribute writes.
        int startShard = ThreadLocalRandom.current()
                .nextInt(shardCount);

        // Try other shards if the selected shard is full.
        for (int attempt = 0; attempt < shardCount; attempt++) {

            int shardId = (startShard + attempt) % shardCount;

            int updated = voucherBatchRepository
                    .tryConsumeRedemptionQuotaOnShard(
                            snapshot.batchId(),
                            snapshot.voucherId(),
                            shardId,
                            now
                    );

            if (updated == 1) {
                return true;
            }
        }

        return false;
    }
}
