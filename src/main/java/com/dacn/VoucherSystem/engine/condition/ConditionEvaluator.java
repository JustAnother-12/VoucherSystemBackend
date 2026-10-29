package com.dacn.VoucherSystem.engine.condition;

import com.dacn.VoucherSystem.engine.context.RuleEvaluationContext;
import com.dacn.VoucherSystem.enums.ConditionType;
import tools.jackson.databind.JsonNode;

public interface ConditionEvaluator {
    ConditionType supportedType();

    boolean evaluate(
            JsonNode config,
            RuleEvaluationContext context
    );
}
