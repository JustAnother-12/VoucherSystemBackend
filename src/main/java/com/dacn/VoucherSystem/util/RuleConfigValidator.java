package com.dacn.VoucherSystem.util;

import com.dacn.VoucherSystem.enums.ConditionType;
import com.dacn.VoucherSystem.enums.RewardType;
import com.dacn.VoucherSystem.exception.InvalidRuleConfigException;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.time.DateTimeException;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.HashSet;
import java.util.Set;

@Component
public class RuleConfigValidator {

    public void validateCondition(
            ConditionType type,
            JsonNode config) {

        validateCondition(type, config, 0);
    }

    private void validateCondition(
            ConditionType type,
            JsonNode config,
            int depth) {

        requireObject(config, "config");

        if (depth > 5) {
            throw new InvalidRuleConfigException(
                    "Maximum condition nesting depth is 5"
            );
        }

        switch (type) {
            case MIN_CART_AMOUNT ->
                    validateMinCartAmount(config);

            case TIME_WINDOW ->
                    validateTimeWindow(config);

            case CUSTOMER_SEGMENT ->
                    validateCustomerSegment(config);

            case COMPOSITE ->
                    validateComposite(config, depth);
        }
    }

    public void validateReward(
            RewardType type,
            JsonNode config) {

        requireObject(config, "config");

        switch (type) {
            case FIXED ->
                    validateFixed(config);

            case PERCENTAGE ->
                    validatePercentage(config);

            case TIERED_PERCENTAGE ->
                    validateTieredPercentage(config);

            case BUY_X_GET_Y ->
                    validateBuyXGetY(config);
        }
    }

    private void requireObject(
            JsonNode node, String path) {

        if (node == null || !node.isObject()) {
            throw new InvalidRuleConfigException(
                    path + " must be a JSON object"
            );
        }
    }

    private JsonNode required(
            JsonNode parent,
            String field,
            String path) {

        JsonNode value = parent.get(field);

        if (value == null || value.isNull()) {
            throw new InvalidRuleConfigException(
                    path + "." + field + " is required"
            );
        }

        return value;
    }

    private String requiredText(
            JsonNode parent,
            String field,
            String path) {

        JsonNode node = required(parent, field, path);

        if (!node.isTextual() || node.textValue().isBlank()) {
            throw new InvalidRuleConfigException(
                    path + "." + field + " must be a non-empty string"
            );
        }

        return node.textValue();
    }

    private BigDecimal requiredNumber(
            JsonNode parent,
            String field,
            String path) {

        JsonNode node = required(parent, field, path);

        if (!node.isNumber()) {
            throw new InvalidRuleConfigException(
                    path + "." + field + " must be a number"
            );
        }

        return node.decimalValue();
    }

    private JsonNode requiredArray(
            JsonNode parent,
            String field,
            String path) {

        JsonNode node = required(parent, field, path);

        if (!node.isArray() || node.size() == 0) {
            throw new InvalidRuleConfigException(
                    path + "." + field + " must be a non-empty array"
            );
        }

        return node;
    }

    private void requireOnlyKeys(
            JsonNode node,
            String path,
            String... allowedKeys) {

        Set<String> allowed = Set.of(allowedKeys);

        node.propertyNames().forEach(field -> {
            if (!allowed.contains(field)) {
                throw new InvalidRuleConfigException(
                        path + "." + field + " is not supported"
                );
            }
        });
    }

    private void validateNonNegativeAmount(
            JsonNode parent,
            String field,
            String parentPath) {

        JsonNode value = parent.get(field);

        String path = parentPath + "." + field;

        if (value == null || value.isNull()) {
            throw new InvalidRuleConfigException(
                    path + " must not be null"
            );
        }

        if (!value.isNumber()) {
            throw new InvalidRuleConfigException(
                    path + " must be a number"
            );
        }

        BigDecimal amount = value.decimalValue();

        if (amount.signum() < 0) {
            throw new InvalidRuleConfigException(
                    path + " must be >= 0"
            );
        }
    }

    private int requirePositiveInteger(
            JsonNode parent,
            String field,
            String parentPath) {

        JsonNode value = required(
                parent, field, parentPath
        );

        String path = parentPath + "." + field;

        if (!value.isIntegralNumber()
                || !value.canConvertToInt()
                || value.intValue() <= 0) {

            throw new InvalidRuleConfigException(
                    path + " must be a positive integer"
            );
        }

        return value.intValue();
    }

    private void validateMinCartAmount(JsonNode config) {
        requireOnlyKeys(
                config, "config",
                "minimumAmount", "amountBasis"
        );

        BigDecimal amount = requiredNumber(
                config, "minimumAmount", "config"
        );

        if (amount.signum() < 0) {
            throw new InvalidRuleConfigException(
                    "config.minimumAmount must be >= 0"
            );
        }

        String basis = requiredText(
                config, "amountBasis", "config"
        );

        if (!"CART_SUBTOTAL".equals(basis)) {
            throw new InvalidRuleConfigException(
                    "config.amountBasis is not supported"
            );
        }
    }

    private void validateTimeWindow(JsonNode config) {
        requireOnlyKeys(
                config, "config", "timezone", "windows"
        );

        String timezone = requiredText(
                config, "timezone", "config"
        );

        try {
            ZoneId.of(timezone);
        } catch (DateTimeException ex) {
            throw new InvalidRuleConfigException(
                    "config.timezone is invalid"
            );
        }

        JsonNode windows = requiredArray(
                config, "windows", "config"
        );

        for (int i = 0; i < windows.size(); i++) {
            JsonNode window = windows.get(i);
            String path = "config.windows[" + i + "]";

            requireObject(window, path);
            requireOnlyKeys(
                    window, path, "startTime", "endTime"
            );

            try {
                LocalTime start = LocalTime.parse(
                        requiredText(window, "startTime", path)
                );

                LocalTime end = LocalTime.parse(
                        requiredText(window, "endTime", path)
                );

                if (!start.isBefore(end)) {
                    throw new InvalidRuleConfigException(
                            path + " startTime must be before endTime"
                    );
                }
            } catch (DateTimeParseException ex) {
                throw new InvalidRuleConfigException(
                        path + " contains an invalid time"
                );
            }
        }
    }

    private void validateCustomerSegment(JsonNode config) {
        requireOnlyKeys(
                config, "config", "operator", "segments"
        );

        String operator = requiredText(
                config, "operator", "config"
        );

        if (!Set.of("IN", "NOT_IN").contains(operator)) {
            throw new InvalidRuleConfigException(
                    "config.operator must be IN or NOT_IN"
            );
        }

        JsonNode segments = requiredArray(
                config, "segments", "config"
        );

        Set<String> uniqueSegments = new HashSet<>();

        for (JsonNode segment : segments) {
            if (!segment.isTextual()
                    || segment.textValue().isBlank()) {
                throw new InvalidRuleConfigException(
                        "config.segments must contain non-empty strings"
                );
            }

            if (!uniqueSegments.add(segment.textValue())) {
                throw new InvalidRuleConfigException(
                        "config.segments must not contain duplicates"
                );
            }
        }
    }

    private void validateComposite(JsonNode config, int depth) {

        requireOnlyKeys(
                config, "config", "operator", "conditions"
        );

        String operator = requiredText(
                config, "operator", "config"
        );

        if (!Set.of("AND", "OR").contains(operator)) {
            throw new InvalidRuleConfigException(
                    "config.operator must be AND or OR"
            );
        }

        JsonNode conditions = requiredArray(
                config, "conditions", "config"
        );

        for (int i = 0; i < conditions.size(); i++) {
            JsonNode child = conditions.get(i);
            String path = "config.conditions[" + i + "]";

            requireObject(child, path);
            requireOnlyKeys(child, path, "type", "config");

            String typeName = requiredText(
                    child, "type", path
            );

            ConditionType childType;

            try {
                childType = ConditionType.valueOf(typeName);
            } catch (IllegalArgumentException ex) {
                throw new InvalidRuleConfigException(
                        path + ".type is not supported"
                );
            }

            JsonNode childConfig = required(child, "config", path);

            validateCondition(childType, childConfig, depth + 1);
        }
    }

    private void validateFixed(JsonNode config) {
        requireOnlyKeys(
                config, "config", "discountValue"
        );

        BigDecimal amount = requiredNumber(
                config, "discountValue", "config"
        );

        if (amount.signum() <= 0) {
            throw new InvalidRuleConfigException(
                    "config.discountValue must be greater than 0"
            );
        }
    }

    private void validatePercentage(JsonNode config) {
        requireOnlyKeys(
                config,
                "config",
                "discountPercent",
                "maxDiscountAmount",
                "amountBasis"
        );

        BigDecimal percent= requiredNumber(
                config, "discountPercent", "config"
        );

        if (percent.signum() <= 0 || percent.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new InvalidRuleConfigException(
                    "config.discountPercent must be in (0, 100]"
            );
        }

        if (!"CART_SUBTOTAL".equals(requiredText(config, "amountBasis", "config"))) {
            throw new InvalidRuleConfigException(
                    "Unsupported amountBasis"
            );
        }

        validateNonNegativeAmount(
                config, "maxDiscountAmount", "config"
        );
    }

    private void validateTieredPercentage(JsonNode config) {
        requireOnlyKeys(
                config,
                "config",
                "calculationMode",
                "amountBasis",
                "tiers"
        );

        if (!"HIGHEST_MATCH".equals(
                requiredText(config, "calculationMode", "config"))) {
            throw new InvalidRuleConfigException(
                    "Unsupported calculationMode"
            );
        }

        if (!"CART_SUBTOTAL".equals(
                requiredText(config, "amountBasis", "config"))) {
            throw new InvalidRuleConfigException(
                    "Unsupported amountBasis"
            );
        }

        JsonNode tiers = requiredArray(config, "tiers", "config");
        BigDecimal previousMinimum = null;

        for (int i = 0; i < tiers.size(); i++) {
            JsonNode tier = tiers.get(i);
            String path = "config.tiers[" + i + "]";

            requireObject(tier, path);
            requireOnlyKeys(
                    tier,
                    path,
                    "minCartAmount",
                    "discountPercent",
                    "maxDiscountAmount"
            );

            BigDecimal minimum = requiredNumber(
                    tier, "minCartAmount", path
            );

            BigDecimal percent = requiredNumber(
                    tier, "discountPercent", path
            );

            if (minimum.signum() < 0) {
                throw new InvalidRuleConfigException(
                        path + ".minCartAmount must be >= 0"
                );
            }

            if (previousMinimum != null
                    && minimum.compareTo(previousMinimum) <= 0) {
                throw new InvalidRuleConfigException(
                        "Tier minimum amounts must be strictly increasing"
                );
            }

            if (percent.signum() <= 0
                    || percent.compareTo(
                    BigDecimal.valueOf(100)) > 0) {
                throw new InvalidRuleConfigException(
                        path + ".discountPercent must be in (0, 100]"
                );
            }

            validateNonNegativeAmount(
                    tier, "maxDiscountAmount", path
            );

            previousMinimum = minimum;
        }
    }

    private void validateBuyXGetY(JsonNode config) {
        requireOnlyKeys(
                config,
                "config",
                "calculationMode",
                "buy",
                "get",
                "maxApplicationsPerOrder"
        );

        String mode = requiredText(
                config, "calculationMode", "config"
        );

        if (!"PER_QUALIFYING_SET".equals(mode)) {
            throw new InvalidRuleConfigException(
                    "Unsupported calculationMode"
            );
        }

        JsonNode buy = required(config, "buy", "config");
        requireObject(buy, "config.buy");

        requireOnlyKeys(
                buy, "config.buy", "productIds", "quantity"
        );

//        validateProductIds(
//                buy, "productIds", "config.buy"
//        );

        requirePositiveInteger(
                buy, "quantity", "config.buy"
        );

        JsonNode get = required(config, "get", "config");
        requireObject(get, "config.get");

        requireOnlyKeys(
                get,
                "config.get",
                "productIds",
                "quantity",
                "discountPercent"
        );

//        validateProductIds(
//                get, "productIds", "config.get"
//        );

        requirePositiveInteger(
                get, "quantity", "config.get"
        );

        BigDecimal discountPercent = requiredNumber(
                get, "discountPercent", "config.get"
        );

        if (discountPercent.signum() <= 0
                || discountPercent.compareTo(
                BigDecimal.valueOf(100)) > 0) {

            throw new InvalidRuleConfigException(
                    "config.get.discountPercent must be in (0, 100]"
            );
        }

        requirePositiveInteger(
                config,
                "maxApplicationsPerOrder",
                "config"
        );
    }

}
