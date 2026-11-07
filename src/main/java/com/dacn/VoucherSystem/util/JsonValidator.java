package com.dacn.VoucherSystem.util;

import com.dacn.VoucherSystem.exception.InvalidJsonException;
import tools.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.util.Set;

public class JsonValidator {
    public static void requireObject(
            JsonNode node, String path) {

        if (node == null || !node.isObject()) {
            throw new InvalidJsonException(
                    path + " must be a JSON object"
            );
        }
    }

    public static JsonNode required(JsonNode parent, String field, String path) {
        if (parent == null || !parent.isObject()) {
            throw new InvalidJsonException(path + " must be a JSON object");
        }
        JsonNode node = parent.get(field);
        if (node == null || node.isNull()) {
            throw new InvalidJsonException(path + "." + field + " is required");
        }
        return node;
    }

    public static String requiredText(JsonNode parent, String field, String path) {
        JsonNode node = required(parent, field, path);
        if (!node.isTextual() || node.textValue().isBlank()) {
            throw new InvalidJsonException(path + "." + field + " must be a non-empty string");
        }
        return node.textValue();
    }

    public static BigDecimal requiredNumber(JsonNode parent, String field, String path) {
        JsonNode node = required(parent, field, path);
        if (!node.isNumber()) throw new InvalidJsonException(path + "." + field + " must be a number");
        return node.decimalValue();
    }

    public static JsonNode requiredArray(JsonNode parent, String field, String path) {
        JsonNode node = required(parent, field, path);
        if (!node.isArray() || node.isEmpty()) {
            throw new InvalidJsonException(path + "." + field + " must be a non-empty array");
        }
        return node;
    }

    public static int requirePositiveInteger(JsonNode parent, String field, String path) {
        JsonNode node = required(parent, field, path);
        if (!node.isIntegralNumber() || !node.canConvertToInt() || node.intValue() <= 0) {
            throw new InvalidJsonException(path + "." + field + " must be a positive integer");
        }
        return node.intValue();
    }

    public static void requireOnlyKeys(
            JsonNode node,
            String path,
            String... allowedKeys) {

        Set<String> allowed = Set.of(allowedKeys);

        node.propertyNames().forEach(field -> {
            if (!allowed.contains(field)) {
                throw new InvalidJsonException(
                        path + "." + field + " is not supported"
                );
            }
        });
    }
}
