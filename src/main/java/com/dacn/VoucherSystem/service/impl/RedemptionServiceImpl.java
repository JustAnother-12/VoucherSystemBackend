package com.dacn.VoucherSystem.service.impl;

import com.dacn.VoucherSystem.cache.VoucherRuleSnapshot;
import com.dacn.VoucherSystem.cache.VoucherSnapshotCache;
import com.dacn.VoucherSystem.dto.request.VoucherRedeemRequest;
import com.dacn.VoucherSystem.dto.response.RedemptionLogResponse;
import com.dacn.VoucherSystem.dto.response.VoucherRedeemResponse;
import com.dacn.VoucherSystem.engine.condition.ConditionEngine;
import com.dacn.VoucherSystem.engine.context.CartItemContext;
import com.dacn.VoucherSystem.engine.context.RuleEvaluationContext;
import com.dacn.VoucherSystem.engine.result.RewardResult;
import com.dacn.VoucherSystem.engine.reward.RewardEngine;
import com.dacn.VoucherSystem.entity.*;
import com.dacn.VoucherSystem.enums.*;
import com.dacn.VoucherSystem.exception.RedemptionConflictException;
import com.dacn.VoucherSystem.exception.ResourceNotFoundException;
import com.dacn.VoucherSystem.exception.VoucherNotApplicableException;
import com.dacn.VoucherSystem.mapper.RedemptionLogMapper;
import com.dacn.VoucherSystem.repository.CampaignRepository;
import com.dacn.VoucherSystem.repository.RedemptionLogRepository;
import com.dacn.VoucherSystem.repository.VoucherRepository;
import com.dacn.VoucherSystem.service.*;
import com.dacn.VoucherSystem.service.interfaces.RedemptionService;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RedemptionServiceImpl implements RedemptionService {
    private final RedemptionLogRepository redemptionLogRepository;
    private final RedemptionLogMapper redemptionLogMapper;
    private final VoucherSnapshotCache snapshotCache;
    private final ConditionEngine conditionEngine;
    private final RewardEngine rewardEngine;
    private final RedemptionCommitService commitService;
    private final RedemptionFailureLogService failureLogService;
    private final VoucherRedemptionLockService lockService;
    private final RedisRateLimitService rateLimitService;
    private final RedemptionFraudGuard fraudGuard;

    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Override
    public List<RedemptionLogResponse> getAllRedemptionLog() {
        List<RedemptionLog> logs = redemptionLogRepository.findAll();

        return logs.stream()
                .map(redemptionLogMapper::toRedemptionLogResponse)
                .toList();
    }

    @Override
    public RedemptionLogResponse getRedemptionLogById(Long id) {
        RedemptionLog log = redemptionLogRepository
                .findById(id)
                .orElseThrow(
                        () -> new ResourceNotFoundException("No redemption log with id " + id + " found in the system")
                );

        return redemptionLogMapper.toRedemptionLogResponse(log);
    }

    @Override
    public VoucherRedeemResponse redeemVoucher(
            String customerId,
            String code,
            BigDecimal cartSubtotal,
            Set<String> customerSegments,
            List<VoucherRedeemRequest.CartItemRequest> items,
            String ipAddress,
            String deviceFingerprint
    ) {
        // check rate limit before any action
        rateLimitService.check(
                "/redemptions",
                customerId,
                ipAddress,
                deviceFingerprint
        );

        // get necessary info from cache
        VoucherRuleSnapshot snapshot = snapshotCache.getByCode(code);

        // check black list
        fraudGuard.verifyAllowed(
                snapshot.voucherId(),
                customerId,
                ipAddress,
                deviceFingerprint
        );

        LocalDateTime now = LocalDateTime.now(clock);
        BigDecimal subtotal = cartSubtotal.setScale(2, RoundingMode.HALF_UP);

        try {
            validateSnapshot(snapshot, now);
            validateGenericVoucherUsage(snapshot, customerId);

            RuleEvaluationContext context = new RuleEvaluationContext(
                    subtotal,
                    customerSegments,
                    Instant.now(clock),
                    items.stream()
                            .map(item -> new CartItemContext(
                                    item.productId(),
                                    item.quantity(),
                                    item.unitPrice()
                            ))
                            .toList()
            );

            JsonNode conditionConfig = objectMapper.readTree(snapshot.conditionJson());

            boolean eligible = conditionEngine.evaluate(
                    snapshot.conditionType(),
                    conditionConfig,
                    context
            );

            if (!eligible) {
                throw new VoucherNotApplicableException(
                        "Cart does not satisfy voucher conditions"
                );
            }

            JsonNode rewardConfig = objectMapper.readTree(snapshot.rewardJson());

            RewardResult reward = rewardEngine.calculate(
                    snapshot.rewardType(),
                    rewardConfig,
                    context
            );

            // perform lock
            return lockService.execute(
                    snapshot.voucherId(),
                    customerId,
                    () -> {
//                        validateGenericVoucherUsage(snapshot, customerId);
                        try {
                            // create success log
                            return commitService.commit(
                                    snapshot,
                                    customerId,
                                    cartSubtotal,
                                    ipAddress,
                                    deviceFingerprint,
                                    reward
                            );

                        } catch (DataIntegrityViolationException ex) {
                            if (isDuplicateRedemptionConstraint(ex)) {
                                throw new RedemptionConflictException(
                                        "Customer has already redeemed this voucher"
                                );
                            }

                            throw ex;
                        }
                    }
            );

        } catch (VoucherNotApplicableException | RedemptionConflictException ex) {
            failureLogService.saveFailed(
                    snapshot.voucherId(),
                    customerId,
                    cartSubtotal,
                    ipAddress,
                    deviceFingerprint,
                    ex.getMessage()
            );

            throw ex;
        }
    }

    private void validateSnapshot(
            VoucherRuleSnapshot snapshot,
            LocalDateTime now
    ) {
        if (snapshot.batchStatus() != BatchStatus.GENERATED) {
            throw new VoucherNotApplicableException(
                    "Voucher batch is not available"
            );
        }

        if (snapshot.campaignStatus() != CampaignStatus.ACTIVE
                || now.isBefore(snapshot.campaignStartTime())
                || !now.isBefore(snapshot.campaignEndTime())) {
            throw new VoucherNotApplicableException(
                    "Campaign is inactive or expired"
            );
        }

        if (snapshot.conditionStatus() != RuleStatus.PUBLISHED
                || snapshot.rewardStatus() != RuleStatus.PUBLISHED) {
            throw new VoucherNotApplicableException(
                    "Voucher rule is not published"
            );
        }
    }

    private void validateGenericVoucherUsage(
            VoucherRuleSnapshot snapshot,
            String customerId
    ) {
        if (snapshot.batchType() != BatchType.GENERIC) {
            return;
        }

        boolean alreadyRedeemed =
                redemptionLogRepository.existsByVoucherIdAndCustomerIdAndStatus(
                        snapshot.voucherId(),
                        customerId,
                        RedemptionStatus.SUCCESS
                );

        if (alreadyRedeemed) {
            throw new RedemptionConflictException(
                    "Customer has already redeemed this voucher"
            );
        }
    }

    private boolean isDuplicateRedemptionConstraint(Throwable ex) {
        while (ex != null) {
            if (ex instanceof ConstraintViolationException violation) {
                return "ux_redemption_logs_success_voucher_customer"
                        .equals(violation.getConstraintName());
            }

            ex = ex.getCause();
        }

        return false;
    }
}
