package com.dacn.VoucherSystem.mapper;

import com.dacn.VoucherSystem.dto.response.BatchRuleResponse;
import com.dacn.VoucherSystem.entity.BatchRule;
import org.springframework.stereotype.Component;

@Component
public class BatchRuleMapper {
    public BatchRuleResponse toBatchRuleResponse(BatchRule rule) {
        return new BatchRuleResponse(
                rule.getId(),
                rule.getReward().getId(),
                rule.getCondition().getId()
        );
    }
}
