package com.dacn.VoucherSystem.dto.request;

import com.dacn.VoucherSystem.enums.ConditionType;
import io.swagger.v3.oas.annotations.media.Schema;
import org.openapitools.jackson.nullable.JsonNullable;
import tools.jackson.databind.JsonNode;

public record ConditionUpdateRequest(
        JsonNullable<String> name,
        JsonNullable<ConditionType> conditionType,
        @Schema(type = "object", example = "{}")
        JsonNullable<JsonNode> config
) {
}
