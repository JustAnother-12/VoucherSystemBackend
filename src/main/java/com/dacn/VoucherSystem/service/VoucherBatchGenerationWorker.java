package com.dacn.VoucherSystem.service;

import com.dacn.VoucherSystem.entity.*;
import com.dacn.VoucherSystem.enums.BatchStatus;
import com.dacn.VoucherSystem.enums.BatchType;
import com.dacn.VoucherSystem.enums.RuleStatus;
import com.dacn.VoucherSystem.event.VoucherSnapshotsChangedEvent;
import com.dacn.VoucherSystem.exception.ResourceNotFoundException;
import com.dacn.VoucherSystem.exception.VoucherBatchTypeException;
import com.dacn.VoucherSystem.repository.*;
import com.dacn.VoucherSystem.util.VoucherCodeGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class VoucherBatchGenerationWorker {
    private static final int CHUNK_SIZE = 1_000;
    public static final int GENERIC_GENERATED_COUNT = 1;

    private final ApplicationEventPublisher eventPublisher;
    private final VoucherBatchRepository voucherBatchRepository;
    private final ConditionRepository conditionRepository;
    private final RewardRepository rewardRepository;
    private final VoucherRepository voucherRepository;
    private final VoucherCodeGenerator voucherCodeGenerator;
    private final Clock clock;

    @Transactional
    public void generate(Long batchId) {
        VoucherBatch batch = voucherBatchRepository.findByIdForUpdate(batchId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No batch with id " + batchId + " found in the system"));

        if (batch.getStatus() != BatchStatus.DRAFT) {
            throw new VoucherBatchTypeException(
                    "Cannot generate vouchers because the batch is already in "
                            + batch.getStatus() + " state.");
        }

        JsonNode codeConfig = batch.getCodeConfigJson();
        JsonNode channels = batch.getDistributionChannelsJson();
        boolean isGeneric = batch.getBatchType() == BatchType.GENERIC;
        int codeCount = isGeneric ? GENERIC_GENERATED_COUNT : Math.toIntExact(totalQuota(channels));

        Set<String> codes = voucherCodeGenerator.generate(
                codeConfig.get("prefix").textValue(),
                codeConfig.get("length").intValue(),
                codeConfig.get("charset").textValue(),
                codeCount,
                this::findExistingCodesChunked);

        List<Voucher> vouchers = codes.stream()
                .map(code -> Voucher.builder()
                        .createdAt(LocalDateTime.now(clock))
                        .batch(batch)
                        .campaign(batch.getCampaign())
                        .code(code)
                        .build())
                .toList();

        voucherRepository.saveAll(vouchers);
        batch.setGeneratedCount(vouchers.size());
        batch.setStatus(BatchStatus.GENERATED);
        publishRuleForVoucherBatch(batch);
        eventPublisher.publishEvent(new VoucherSnapshotsChangedEvent());
    }

    private long totalQuota(JsonNode channels) {
        long total = 0;
        for (JsonNode allocation : channels.get("allocations")) {
            total += allocation.get("quota").intValue();
        }
        return total;
    }

    private Set<String> findExistingCodesChunked(Collection<String> codes) {
        Set<String> existing = new HashSet<>();
        List<String> list = new ArrayList<>(codes);
        for (int i = 0; i < list.size(); i += CHUNK_SIZE) {
            existing.addAll(voucherRepository.findExistingCodes(
                    list.subList(i, Math.min(i + CHUNK_SIZE, list.size()))));
        }
        return existing;
    }

    private void publishRuleForVoucherBatch(VoucherBatch batch) {
        Condition condition = conditionRepository
                .findById(batch.getRule().getCondition().getId())
                .orElseThrow(
                        () -> new ResourceNotFoundException("No condition found for VoucherBatch's Rule")
                );
        Reward reward = rewardRepository
                .findById(batch.getRule().getReward().getId())
                .orElseThrow(
                        () -> new ResourceNotFoundException("No reward found for VoucherBatch's Rule")
                );

        condition.setStatus(RuleStatus.PUBLISHED);
        reward.setStatus(RuleStatus.PUBLISHED);
        conditionRepository.save(condition);
        rewardRepository.save(reward);
    }
}
