package com.dacn.VoucherSystem.service.impl;

import com.dacn.VoucherSystem.dto.response.VoucherBatchResponse;
import com.dacn.VoucherSystem.entity.*;
import com.dacn.VoucherSystem.enums.BatchStatus;
import com.dacn.VoucherSystem.enums.BatchType;
import com.dacn.VoucherSystem.enums.DurationUnit;
import com.dacn.VoucherSystem.event.VoucherSnapshotsChangedEvent;
import com.dacn.VoucherSystem.exception.InvalidBatchConfigException;
import com.dacn.VoucherSystem.exception.ResourceNotFoundException;
import com.dacn.VoucherSystem.exception.VoucherBatchTypeException;
import com.dacn.VoucherSystem.mapper.VoucherBatchMapper;
import com.dacn.VoucherSystem.repository.*;
import com.dacn.VoucherSystem.service.VoucherBatchGenerationService;
import com.dacn.VoucherSystem.service.VoucherBatchGenerationWorker;
import com.dacn.VoucherSystem.service.interfaces.VoucherBatchService;
import com.dacn.VoucherSystem.util.BatchConfigValidator;
import lombok.RequiredArgsConstructor;
import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import tools.jackson.databind.JsonNode;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class VoucherBatchServiceImpl implements VoucherBatchService {
    private final ApplicationEventPublisher eventPublisher;
    private final VoucherBatchRepository voucherBatchRepository;
    private final CampaignRepository campaignRepository;
    private final BatchRuleRepository batchRuleRepository;
    private final BatchConfigValidator batchConfigValidator;
    private final VoucherBatchMapper voucherBatchMapper;
    private final Clock clock;
    private final VoucherBatchGenerationService voucherBatchGenerationService;
    private final Set<Long> generatingBatchIds = ConcurrentHashMap.newKeySet();

    @Override
    @Transactional(readOnly = true)
    public List<VoucherBatchResponse> getAllVoucherBatches(){
        List<VoucherBatch> voucherBatches = voucherBatchRepository.findAll();

        return voucherBatches.stream()
                .map(voucherBatchMapper::toVoucherBatchResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public VoucherBatchResponse getVoucherBatchByID(Long id){
        VoucherBatch voucherBatch = voucherBatchRepository.
                findById(id)
                .orElseThrow(
                () -> new ResourceNotFoundException("No batch with id " + id + " found in the system")
        );
        return voucherBatchMapper.toVoucherBatchResponse(voucherBatch);
    }

    @Override
    @Transactional
    public VoucherBatchResponse createVoucherBatch(
            Long campaignId,
            Long ruleId,
            BatchType batchType,
            JsonNode codeConfig,
            DurationUnit durationUnit,
            Integer durationValue,
            Integer maxUsageCount,
            JsonNode distributionChannels
    ){
        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(
                () -> new ResourceNotFoundException("No campaign with id " + campaignId + " found in the system")
        );

        BatchRule batchRule = batchRuleRepository.findById(ruleId)
                .orElseThrow(
                () -> new ResourceNotFoundException("No batch rule with id " + ruleId + " found in the system")
        );

        batchConfigValidator.validateCodeConfig(codeConfig);
        batchConfigValidator.validateDistributionChannels(distributionChannels, batchType == BatchType.GENERIC);

        if (durationUnit == null) {
            throw new InvalidBatchConfigException(
                    "durationUnit is required"
            );
        }

        if (durationValue == null || durationValue <= 0) {
            throw new InvalidBatchConfigException(
                    "durationValue must be greater than 0"
            );
        }

        BatchQuota quota = resolveQuota(batchType, distributionChannels, maxUsageCount);

        VoucherBatch voucherBatch =
                VoucherBatch.builder()
                        .createdAt(LocalDateTime.now(clock))
                        .campaign(campaign)
                        .rule(batchRule)
                        .batchType(batchType)
                        .codeConfigJson(codeConfig)
                        .durationUnit(durationUnit)
                        .durationValue(durationValue)
                        .distributionChannelsJson(quota.distributionChannels())
                        .generatedCount(quota.generatedCount())
                        .maxUsageCount(quota.maxUsageCount())
                        .status(BatchStatus.DRAFT)
                        .build();

        voucherBatchRepository.saveAndFlush(voucherBatch);
        return voucherBatchMapper.toVoucherBatchResponse(voucherBatch);
    }

    @Override
    @Transactional
    public VoucherBatchResponse updateVoucherBatch(
            Long id,
            JsonNullable<Long> ruleId,
            JsonNullable<BatchType> batchType,
            JsonNullable<JsonNode> codeConfig,
            JsonNullable<DurationUnit> durationUnit,
            JsonNullable<Integer> durationValue,
            JsonNullable<Integer> maxUsageCount,
            JsonNullable<JsonNode> distributionChannels,
            JsonNullable<BatchStatus> status
    ){
        VoucherBatch batch = voucherBatchRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No batch with id " + id + " found in the system"));

        validateEditable(batch);

        if (ruleId.isPresent()) {
            Long newRuleId = ruleId.get();
            if (!newRuleId.equals(batch.getRule().getId())) {
                BatchRule newRule = batchRuleRepository.findById(newRuleId)
                        .orElseThrow(() -> new ResourceNotFoundException("No batch rule with id " + newRuleId + " found in the system"));
                batch.setRule(newRule);
            }
        }

        if (batchType.isPresent()) {
            batch.setBatchType(batchType.get());
        }

        if (codeConfig.isPresent()) {
            batchConfigValidator.validateCodeConfig(codeConfig.get());
            batch.setCodeConfigJson(codeConfig.get());
        }

        if (durationUnit.isPresent()) {
            batch.setDurationUnit(durationUnit.get());
        }

        if (durationValue.isPresent()) {
            if (durationValue.get() <= 0) {
                throw new InvalidBatchConfigException(
                        "durationValue must be greater than 0"
                );
            }
            batch.setDurationValue(durationValue.get());
        }

        BatchType effectiveType = batchType.isPresent() ? batchType.get() : batch.getBatchType();
        JsonNode effectiveChannels = distributionChannels.isPresent()? distributionChannels.get(): batch.getDistributionChannelsJson();
        Integer effectiveMaxUsage = maxUsageCount.isPresent()? maxUsageCount.get(): batch.getMaxUsageCount();

        if (distributionChannels.isPresent()) {
            batchConfigValidator.validateDistributionChannels(effectiveChannels, effectiveType == BatchType.GENERIC);
        }

        BatchQuota quota = resolveQuota(effectiveType, effectiveChannels, effectiveMaxUsage);
        batch.setBatchType(effectiveType);
        batch.setDistributionChannelsJson(quota.distributionChannels());
        batch.setGeneratedCount(quota.generatedCount());
        batch.setMaxUsageCount(quota.maxUsageCount());

        if (status.isPresent() && status.get() == BatchStatus.GENERATED) {
            scheduleVoucherGeneration(batch.getId());
        }

        eventPublisher.publishEvent(
                new VoucherSnapshotsChangedEvent()
        );

        return voucherBatchMapper.toVoucherBatchResponse(batch);

    }

    /**
     * GENERIC: generatedCount = 1, maxUsageCount lấy từ request (bắt buộc > 0),
     *          quota của mọi kênh = 0 vì mã chung được phân phối tùy ý.
     * BULK:    generatedCount = maxUsageCount = tổng quota, bỏ qua maxUsageCount từ request.
     */
    private BatchQuota resolveQuota(BatchType batchType, JsonNode distributionChannels, Integer maxUsageCount) {
        if (batchType == BatchType.GENERIC) {
            if (maxUsageCount == null || maxUsageCount <= 0) {
                throw new InvalidBatchConfigException("maxUsageCount must be greater than 0 for GENERIC batch");
            }
            return new BatchQuota(
                    VoucherBatchGenerationWorker.GENERIC_GENERATED_COUNT,
                    maxUsageCount,
                    batchConfigValidator.resetChannelQuotas(distributionChannels));
        }

        long total = totalQuota(distributionChannels);
        if (total <= 0) {
            throw new InvalidBatchConfigException("distributionChannels total quota must be greater than 0 for BULK batch");
        }
        int quota = Math.toIntExact(total);
        return new BatchQuota(quota, quota, distributionChannels);
    }

    private record BatchQuota(int generatedCount, int maxUsageCount, JsonNode distributionChannels) {}

    private long totalQuota(JsonNode channels) {
        long total = 0;
        for (JsonNode allocation : channels.get("allocations")) {
            total += allocation.get("quota").intValue();
        }
        return total;
    }

    private void scheduleVoucherGeneration(Long batchId) {
        if (!generatingBatchIds.add(batchId)) {
            throw new VoucherBatchTypeException(
                    "Voucher generation is already in progress for batch " + batchId);
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    voucherBatchGenerationService.generate(batchId)
                            .whenComplete((result, error) -> generatingBatchIds.remove(batchId));
                } catch (RuntimeException exception) {
                    generatingBatchIds.remove(batchId);
                    throw exception;
                }
            }

            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) {
                    generatingBatchIds.remove(batchId);
                }
            }
        });
    }

    private void validateEditable(VoucherBatch batch) {
        if (generatingBatchIds.contains(batch.getId())) {
            throw new VoucherBatchTypeException(
                    "Cannot update voucher batch because voucher generation is in progress.");
        }
        if (batch.getStatus() != BatchStatus.DRAFT) {
            throw new VoucherBatchTypeException(
                    "Cannot update voucher batch because it is already in "
                            + batch.getStatus() + " state.");
        }
    }

}
