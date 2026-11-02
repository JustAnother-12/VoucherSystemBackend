package com.dacn.VoucherSystem.engine.reward;

import com.dacn.VoucherSystem.engine.context.RuleEvaluationContext;
import com.dacn.VoucherSystem.engine.result.RewardResult;
import com.dacn.VoucherSystem.enums.RewardType;
import tools.jackson.databind.JsonNode;

public interface RewardCalculator {
    RewardType supportedType();

    RewardResult calculate(
            JsonNode config,
            RuleEvaluationContext context
    );
}
