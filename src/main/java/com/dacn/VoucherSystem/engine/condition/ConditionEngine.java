package com.dacn.VoucherSystem.engine.condition;

import com.dacn.VoucherSystem.engine.context.RuleEvaluationContext;
import com.dacn.VoucherSystem.enums.ConditionType;
import com.dacn.VoucherSystem.exception.InvalidRuleConfigException;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static com.dacn.VoucherSystem.util.JsonValidator.*;

@Component
public class ConditionEngine {
    public static final int MAX_COMPOSITE_DEPTH = 5;
    private final Map<ConditionType, ConditionEvaluator> evaluators = new EnumMap<>(ConditionType.class);

    public ConditionEngine(List<ConditionEvaluator> evaluatorList) {
        for (ConditionEvaluator evaluator : evaluatorList) {
            ConditionEvaluator old = evaluators.put(evaluator.supportedType(), evaluator);
            if (old != null)
                throw new IllegalStateException("Duplicate ConditionEvaluator for " + evaluator.supportedType());
        }
    }

    public boolean evaluate(ConditionType type, JsonNode config, RuleEvaluationContext context) {
        return evaluate(type, config, context, 0);
    }

    private boolean evaluate(
            ConditionType type,
            JsonNode config,
            RuleEvaluationContext context,
            int depth
    ) {
        if (type == null || config == null || !config.isObject()) {
            throw new InvalidRuleConfigException("Condition type and object config are required");
        }
        if (depth > MAX_COMPOSITE_DEPTH) {
            throw new InvalidRuleConfigException("Maximum condition nesting depth is " + MAX_COMPOSITE_DEPTH);
        }
        if (type != ConditionType.COMPOSITE) {
            ConditionEvaluator evaluator = evaluators.get(type);
            if (evaluator == null) throw new IllegalStateException("No evaluator registered for " + type);
            return evaluator.evaluate(config, context);
        }

        // evaluate for COMPOSITE
        String operator = requiredText(config, "operator", "config");
        JsonNode children = requiredArray(config, "conditions", "config");
        if (!operator.equals("AND") && !operator.equals("OR")) {
            throw new InvalidRuleConfigException("config.operator must be AND or OR");
        }

        if (operator.equals("AND")) {
            for (JsonNode child : children) {
                ConditionType childType = parseType(child);
                if (!evaluate(childType, required(child, "config", "condition"), context, depth + 1)) {
                    return false; // AND short-circuit
                }
            }
            return true;
        }
        for (JsonNode child : children) {
            ConditionType childType = parseType(child);
            if (evaluate(childType, required(child, "config", "condition"), context, depth + 1)) {
                return true; // OR short-circuit
            }
        }
        return false;
    }

    private ConditionType parseType(JsonNode child) {
        String name = requiredText(child, "type", "condition");
        try {
            return ConditionType.valueOf(name);
        } catch (IllegalArgumentException ex) {
            throw new InvalidRuleConfigException("Unsupported condition type: " + name);
        }
    }
}
