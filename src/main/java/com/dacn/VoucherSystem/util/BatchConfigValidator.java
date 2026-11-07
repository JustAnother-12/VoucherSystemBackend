package com.dacn.VoucherSystem.util;

import com.dacn.VoucherSystem.exception.InvalidBatchConfigException;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import static com.dacn.VoucherSystem.util.JsonValidator.*;

@Component
public class BatchConfigValidator {
    private static final int MAX_CODES_PER_BATCH = 100_000;
    public static final int MAX_CODE_LENGTH = 50;
    private static final int MIN_RANDOM_LENGTH = 4;
    private static final int MAX_RANDOM_LENGTH = 32;
    private static final Pattern PREFIX_PATTERN = Pattern.compile("[A-Za-z0-9_-]+");
    private final ObjectMapper objectMapper = new ObjectMapper();

    public void validateCodeConfig(JsonNode config) {
        requireObject(config, "codeConfig");
        requireOnlyKeys(config, "codeConfig", "prefix", "length", "charset");

        String prefix = requiredText(config, "prefix", "codeConfig");
        if (!PREFIX_PATTERN.matcher(prefix).matches()) {
            throw new InvalidBatchConfigException(
                    "codeConfig.prefix may only contain letters, digits, '-' and '_'");
        }

        int length = requirePositiveInteger(config, "length", "codeConfig");
        if (length < MIN_RANDOM_LENGTH || length > MAX_RANDOM_LENGTH) {
            throw new InvalidBatchConfigException(
                    "codeConfig.length must be in [" + MIN_RANDOM_LENGTH + ", " + MAX_RANDOM_LENGTH + "]");
        }

        if (prefix.length() + length > MAX_CODE_LENGTH) {
            throw new InvalidBatchConfigException(
                    "codeConfig.prefix + length must be at most " + MAX_CODE_LENGTH + " characters");
        }

        String charset = requiredText(config, "charset", "codeConfig");
        if (!VoucherCodeGenerator.SUPPORTED_CHARSETS.contains(charset)) {
            throw new InvalidBatchConfigException(
                    "codeConfig.charset must be one of " + VoucherCodeGenerator.SUPPORTED_CHARSETS);
        }
    }

    public void validateDistributionChannels(JsonNode config, boolean allowZeroTotal) {
        requireObject(config, "distributionChannels");
        requireOnlyKeys(config, "distributionChannels", "allocations");

        JsonNode allocations = config.get("allocations");
        if (allocations == null || !allocations.isArray() || allocations.isEmpty()) {
            throw new InvalidBatchConfigException(
                    "distributionChannels.allocations must be a non-empty array");
        }

        Set<String> seen = new HashSet<>();
        long total = 0;

        for (int i = 0; i < allocations.size(); i++) {
            JsonNode item = allocations.get(i);
            String path = "distributionChannels.allocations[" + i + "]";

            requireObject(item, path);
            requireOnlyKeys(item, path, "channel", "quota");

            String channel = requiredText(item, "channel", path);
            // TODO: if channel is enum then validate
            if (!seen.add(channel)) {
                throw new InvalidBatchConfigException(path + ".channel must not be duplicated");
            }

            JsonNode quota = required(item, "quota", path);
            if (!quota.isIntegralNumber() || !quota.canConvertToInt() || quota.intValue() < 0) {
                throw new InvalidBatchConfigException(path + ".quota must be a non-negative integer");
            }
            total += quota.intValue();
        }

        if (total <= 0 && !allowZeroTotal) {
            throw new InvalidBatchConfigException(
                    "distributionChannels total quota must be greater than 0");
        }
        if (total > MAX_CODES_PER_BATCH) {
            throw new InvalidBatchConfigException(
                    "distributionChannels total quota must be in [1, " + MAX_CODES_PER_BATCH + "], got " + total);
        }
    }

    /**
     * GENERIC: đặt quota của mọi kênh về 0. Trả về bản sao, không sửa JsonNode đầu vào.
     */
    public JsonNode resetChannelQuotas(JsonNode config) {
        ObjectNode copy = (ObjectNode) objectMapper.readTree(config.toString());
        for (JsonNode allocation : copy.get("allocations")) {
            ((ObjectNode) allocation).put("quota", 0);
        }
        return copy;
    }

    private static void requireObject(JsonNode node, String path) {
        if (node == null || !node.isObject()) {
            throw new InvalidBatchConfigException(path + " must be a JSON object");
        }
    }

    private static void requireOnlyKeys(JsonNode node, String path, String... allowedKeys) {
        Set<String> allowed = Set.of(allowedKeys);
        node.propertyNames().forEach(field -> {
            if (!allowed.contains(field)) {
                throw new InvalidBatchConfigException(path + "." + field + " is not supported");
            }
        });
    }

    private static JsonNode required(JsonNode parent, String field, String path) {
        JsonNode node = parent.get(field);
        if (node == null || node.isNull()) {
            throw new InvalidBatchConfigException(path + "." + field + " is required");
        }
        return node;
    }

    private static String requiredText(JsonNode parent, String field, String path) {
        JsonNode node = required(parent, field, path);
        if (!node.isTextual() || node.textValue().isBlank()) {
            throw new InvalidBatchConfigException(path + "." + field + " must be a non-empty string");
        }
        return node.textValue();
    }

    private static int requiredPositiveInt(JsonNode parent, String field, String path) {
        JsonNode node = required(parent, field, path);
        if (!node.isIntegralNumber() || !node.canConvertToInt() || node.intValue() <= 0) {
            throw new InvalidBatchConfigException(path + "." + field + " must be a positive integer");
        }
        return node.intValue();
    }


}