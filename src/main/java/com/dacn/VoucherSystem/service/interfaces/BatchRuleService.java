package com.dacn.VoucherSystem.service.interfaces;

import com.dacn.VoucherSystem.dto.response.BatchRuleResponse;
import org.openapitools.jackson.nullable.JsonNullable;

import java.util.List;

public interface BatchRuleService {
    List<BatchRuleResponse> getAllBatchRules(Long rewardId, Long conditionId);
    BatchRuleResponse getBatchRuleById(Long id);
    BatchRuleResponse createBatchRule(
            Long rewardId,
            Long conditionId
    );
    BatchRuleResponse updateBatchRule(
            Long id,
            JsonNullable<Long> rewardId,
            JsonNullable<Long> conditionId
    );
}
