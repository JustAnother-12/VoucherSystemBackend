package com.dacn.VoucherSystem.engine.condition.evaluator;

import com.dacn.VoucherSystem.engine.condition.ConditionEvaluator;
import com.dacn.VoucherSystem.engine.context.RuleEvaluationContext;
import com.dacn.VoucherSystem.enums.ConditionType;
import com.dacn.VoucherSystem.exception.InvalidRuleConfigException;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

import java.time.LocalTime;
import java.time.ZoneId;

import static com.dacn.VoucherSystem.util.JsonValidator.*;

@Component
public class TimeWindowEvaluator implements ConditionEvaluator {
    @Override
    public ConditionType supportedType() {
        return ConditionType.TIME_WINDOW;
    }

    @Override
    public boolean evaluate(JsonNode config, RuleEvaluationContext context) {
        ZoneId zone = ZoneId.of(requiredText(config, "timezone", "config"));
        LocalTime now = context.evaluatedAt().atZone(zone).toLocalTime();
        JsonNode windows = requiredArray(config, "windows", "config");

        for (JsonNode window : windows) {
            LocalTime start = LocalTime.parse(requiredText(window, "startTime", "config.windows[]"));
            LocalTime end = LocalTime.parse(requiredText(window, "endTime", "config.windows[]"));
            if (start.equals(end)) {
                throw new InvalidRuleConfigException(
                        "A time window startTime and endTime must differ");
            }
            // Window semantics are [start, end). Also supports a window crossing midnight.
            boolean matches = start.isBefore(end)
                    ? !now.isBefore(start) && now.isBefore(end)
                    : !now.isBefore(start) || now.isBefore(end);
            if (matches) return true;
        }
        return false;
    }
}
