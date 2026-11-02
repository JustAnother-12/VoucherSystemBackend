package com.dacn.VoucherSystem.engine.condition.evaluator;

import com.dacn.VoucherSystem.engine.condition.ConditionEvaluator;
import com.dacn.VoucherSystem.engine.context.RuleEvaluationContext;
import com.dacn.VoucherSystem.enums.ConditionType;
import com.dacn.VoucherSystem.util.RuleConfigValidator;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

import java.math.BigDecimal;

@Component
public class MinCartAmountEvaluator implements ConditionEvaluator {
    @Override
    public ConditionType supportedType() {
        return ConditionType.MIN_CART_AMOUNT;
    }

    @Override
    public boolean evaluate(JsonNode config, RuleEvaluationContext context) {
        BigDecimal minimumAmount = RuleConfigValidator.requiredNumber(
                config, "minimumAmount", "config"
        );

        return context.cartSubtotal().compareTo(minimumAmount) >= 0;
    }
}
