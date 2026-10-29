package com.dacn.VoucherSystem.engine.condition.evaluator;

import com.dacn.VoucherSystem.engine.condition.ConditionEvaluator;
import com.dacn.VoucherSystem.engine.context.RuleEvaluationContext;
import com.dacn.VoucherSystem.enums.ConditionType;
import com.dacn.VoucherSystem.exception.InvalidRuleConfigException;
import com.dacn.VoucherSystem.util.RuleConfigValidator;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

import java.util.HashSet;
import java.util.Set;

@Component
public class CustomerSegmentEvaluator implements ConditionEvaluator {
    @Override
    public ConditionType supportedType() {
        return ConditionType.CUSTOMER_SEGMENT;
    }

    @Override
    public boolean evaluate(JsonNode config, RuleEvaluationContext context) {
        String operator = RuleConfigValidator.requiredText(config, "operator", "config");
        JsonNode configuredSegments = RuleConfigValidator.requiredArray(config, "segments", "config");
        Set<String> expected = new HashSet<>();
        for (JsonNode node : configuredSegments) {
            if (!node.isTextual() || node.textValue().isBlank()) {
                throw new InvalidRuleConfigException(
                        "config.segments must contain non-empty strings");
            }
            expected.add(node.textValue());
        }
        boolean anyMatch = context.customerSegments().stream().anyMatch(expected::contains);
        return switch (operator) {
            case "IN" -> anyMatch;
            case "NOT_IN" -> !anyMatch;
            default -> throw new InvalidRuleConfigException(
                    "config.operator must be IN or NOT_IN");
        };
    }
}
