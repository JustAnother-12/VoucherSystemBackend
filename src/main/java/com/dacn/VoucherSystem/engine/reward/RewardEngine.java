package com.dacn.VoucherSystem.engine.reward;

import com.dacn.VoucherSystem.engine.context.RuleEvaluationContext;
import com.dacn.VoucherSystem.engine.result.RewardResult;
import com.dacn.VoucherSystem.enums.RewardType;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class RewardEngine {
    private final Map<RewardType, RewardCalculator> calculators = new EnumMap<>(RewardType.class);

    public RewardEngine(List<RewardCalculator> calculatorList) {
        for (RewardCalculator calculator : calculatorList) {
            RewardCalculator old = calculators.put(calculator.supportedType(), calculator);
            if (old != null) throw new IllegalStateException(
                    "Duplicate RewardCalculator for " + calculator.supportedType());
        }
        for (RewardType type : RewardType.values()) {
            if (!calculators.containsKey(type)) throw new IllegalStateException(
                    "No RewardCalculator registered for " + type);
        }
    }

    public RewardResult calculate(RewardType type, JsonNode config, RuleEvaluationContext context) {
        RewardCalculator calculator = calculators.get(type);
        if (calculator == null) throw new IllegalArgumentException("Unsupported reward type: " + type);
        RewardResult result = calculator.calculate(config, context);
        if (result.discountAmount().compareTo(context.cartSubtotal()) > 0) {
            throw new IllegalStateException("Reward calculator returned discount greater than cart subtotal");
        }
        return result;
    }
}
