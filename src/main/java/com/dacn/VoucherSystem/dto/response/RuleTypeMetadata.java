package com.dacn.VoucherSystem.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.v3.oas.annotations.media.Schema;
import tools.jackson.databind.JsonNode;

@JsonPropertyOrder({
        "type",
        "label",
        "description",
        "configSchema"
})
public record RuleTypeMetadata(
        String type,
        String label,
        String description,
        @Schema(type = "object", example = "{}")
        JsonNode configSchema
//        @JsonProperty("ui_schema")
//        @Schema(type = "object", example = "{}")
//        JsonNode uiSchema
) {}
