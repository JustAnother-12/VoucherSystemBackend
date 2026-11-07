package com.dacn.VoucherSystem.service.impl;

import com.dacn.VoucherSystem.dto.response.VoucherBatchResponse;
import com.dacn.VoucherSystem.entity.*;
import com.dacn.VoucherSystem.enums.BatchStatus;
import com.dacn.VoucherSystem.enums.BatchType;
import com.dacn.VoucherSystem.enums.DurationUnit;
import com.dacn.VoucherSystem.enums.RuleStatus;
import com.dacn.VoucherSystem.exception.InvalidBatchConfigException;
import com.dacn.VoucherSystem.exception.ResourceNotFoundException;
import com.dacn.VoucherSystem.exception.VoucherBatchTypeException;
import com.dacn.VoucherSystem.mapper.VoucherBatchMapper;
import com.dacn.VoucherSystem.repository.*;
import com.dacn.VoucherSystem.service.interfaces.VoucherBatchService;
import com.dacn.VoucherSystem.util.BatchConfigValidator;
import com.dacn.VoucherSystem.util.VoucherCodeGenerator;
import lombok.RequiredArgsConstructor;
import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;

import java.util.*;
import java.time.LocalDateTime;
import java.time.Clock;

@Service
@RequiredArgsConstructor
public class VoucherBatchServiceImpl implements VoucherBatchService {
    private static final int CHUNK_SIZE = 1_000;
    private static final int GENERIC_GENERATED_COUNT = 1;

    private final VoucherBatchRepository voucherBatchRepository;
    private final CampaignRepository campaignRepository;
    private final BatchRuleRepository batchRuleRepository;
    private final ConditionRepository conditionRepository;
    private final RewardRepository rewardRepository;
    private final VoucherRepository voucherRepository;
    private final VoucherCodeGenerator voucherCodeGenerator;
    private final BatchConfigValidator batchConfigValidator;
    private final VoucherBatchMapper voucherBatchMapper;
    private final Clock clock;

    @Override
    public List<VoucherBatchResponse> getAllVoucherBatches(){
        List<VoucherBatch> voucherBatches = voucherBatchRepository.findAll();

        return voucherBatches.stream()
                .map(voucherBatchMapper::toVoucherBatchResponse)
                .toList();
    }

    @Override
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
                        .redeemedCount(0)
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
            generateVouchers(batch);
            publishRuleForVoucherBatch(batch);
        }

        return voucherBatchMapper.toVoucherBatchResponse(batch);

    }

    private void generateVouchers(VoucherBatch batch) {
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
            return new BatchQuota(GENERIC_GENERATED_COUNT, maxUsageCount, batchConfigValidator.resetChannelQuotas(distributionChannels));
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

    private Set<String> findExistingCodesChunked(Collection<String> codes) {
        Set<String> existing = new HashSet<>();
        List<String> list = new ArrayList<>(codes);
        for (int i = 0; i < list.size(); i += CHUNK_SIZE) {
            existing.addAll(voucherRepository.findExistingCodes(
                    list.subList(i, Math.min(i + CHUNK_SIZE, list.size()))));
        }
        return existing;
    }

    private void validateEditable(VoucherBatch batch) {
        if (batch.getStatus() != BatchStatus.DRAFT) {
            throw new VoucherBatchTypeException(
                    "Cannot update voucher batch because it is already in "
                            + batch.getStatus() + " state.");
        }
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
