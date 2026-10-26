package com.dacn.VoucherSystem.dto.response;

import tools.jackson.databind.JsonNode;

public record RuleTypeMetadata(
        String type,
        String label,
        String description,
        JsonNode configSchema,
        JsonNode uiSchema
) {}
