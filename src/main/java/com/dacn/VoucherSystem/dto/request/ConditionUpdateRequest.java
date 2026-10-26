package com.dacn.VoucherSystem.dto.request;

import com.dacn.VoucherSystem.enums.ConditionType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.openapitools.jackson.nullable.JsonNullable;
import tools.jackson.databind.JsonNode;

@Data
public class ConditionUpdateRequest {
    private JsonNullable<String> name;
    private JsonNullable<ConditionType> type;
    @Schema(type = "object", example = "{}")
    private JsonNullable<JsonNode> config;
}
