package com.dacn.VoucherSystem.service.impl;

import com.dacn.VoucherSystem.dto.response.BatchRuleResponse;
import com.dacn.VoucherSystem.entity.BatchRule;
import com.dacn.VoucherSystem.entity.Condition;
import com.dacn.VoucherSystem.entity.Reward;
import com.dacn.VoucherSystem.enums.BatchStatus;
import com.dacn.VoucherSystem.exception.InvalidBatchRuleException;
import com.dacn.VoucherSystem.exception.ResourceNotFoundException;
import com.dacn.VoucherSystem.mapper.BatchRuleMapper;
import com.dacn.VoucherSystem.repository.BatchRuleRepository;
import com.dacn.VoucherSystem.repository.ConditionRepository;
import com.dacn.VoucherSystem.repository.RewardRepository;
import com.dacn.VoucherSystem.repository.VoucherBatchRepository;
import com.dacn.VoucherSystem.service.interfaces.BatchRuleService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.time.LocalDateTime;
import java.time.Clock;

@Service
@RequiredArgsConstructor
public class BatchRuleServiceImpl implements BatchRuleService {

    private final BatchRuleRepository batchRuleRepository;
    private final VoucherBatchRepository voucherBatchRepository;
    private final RewardRepository rewardRepository;
    private final ConditionRepository conditionRepository;
    private final BatchRuleMapper mapper;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public List<BatchRuleResponse> getAllBatchRules(Long rewardId, Long conditionId) {
        Specification<BatchRule> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (rewardId != null) {
                predicates.add(cb.equal(root.get("reward").get("id"), rewardId));
            }
            if (conditionId != null) {
                predicates.add(cb.equal(root.get("condition").get("id"), conditionId));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return batchRuleRepository.findAll(spec).stream()
                .map(mapper::toBatchRuleResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public BatchRuleResponse getBatchRuleById(Long id) {
        return mapper.toBatchRuleResponse(findRule(id));
    }

    @Override
    @Transactional
    public BatchRuleResponse createBatchRule(
            Long rewardId,
            Long conditionId
    ) {
        Reward reward = findReward(rewardId);
        Condition condition = findCondition(conditionId);

        if (batchRuleRepository.existsByRewardIdAndConditionId(
                rewardId, conditionId)) {
            throw new InvalidBatchRuleException(
                    "A batch rule with this reward and condition already exists.");
        }
        BatchRule batchRule = BatchRule.builder()
                .createdAt(LocalDateTime.now(clock))
                .reward(reward)
                .condition(condition)
                .build();

        batchRuleRepository.saveAndFlush(batchRule);
        return mapper.toBatchRuleResponse(batchRule);
    }

    @Override
    @Transactional
    public BatchRuleResponse updateBatchRule(
            Long id,
            JsonNullable<Long> rewardId,
            JsonNullable<Long> conditionId
    ) {
        BatchRule rule = findRule(id);

        if (voucherBatchRepository.existsByRule_IdAndStatus(id, BatchStatus.GENERATED)) {
            throw new InvalidBatchRuleException(
                    "Cannot update a rule that is currently in use by a generated voucher batch.");
        }

        Long currentRewardId = rule.getReward().getId();
        Long currentConditionId = rule.getCondition().getId();
        Long newRewardId = currentRewardId;
        Long newConditionId = currentConditionId;

        if (rewardId.isPresent()) {
            newRewardId = rewardId.get();
            if (newRewardId == null) {
                throw new InvalidBatchRuleException("rewardId must not null");
            }
        }

        if (conditionId.isPresent()) {
            newConditionId = conditionId.get();
            if (newConditionId == null) {
                throw new InvalidBatchRuleException("conditionId must not null");
            }
        }

        if (newRewardId.equals(currentRewardId) && newConditionId.equals(currentConditionId)) {
            return mapper.toBatchRuleResponse(rule);
        }

        if (!newRewardId.equals(currentRewardId)) {
            rule.setReward(findReward(newRewardId));
        }
        if (!newConditionId.equals(currentConditionId)) {
            rule.setCondition(findCondition(newConditionId));
        }

        return mapper.toBatchRuleResponse(batchRuleRepository.save(rule));
    }

    private BatchRule findRule(Long id) {
        return batchRuleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No batch rule with id " + id + " found in the system"));
    }

    private Reward findReward(Long id) {
        return rewardRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No Reward with id " + id + " found in the system"));
    }

    private Condition findCondition(Long id) {
        return conditionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No Condition with id " + id + " found in the system"));
    }
}
