package com.dacn.VoucherSystem.dto.request;

import com.dacn.VoucherSystem.enums.ConditionType;
import lombok.Data;
import org.openapitools.jackson.nullable.JsonNullable;
import tools.jackson.databind.JsonNode;

@Data
public class ConditionUpdateRequest {
    private JsonNullable<String> name;
    private JsonNullable<ConditionType> type;
    private JsonNullable<JsonNode> config;
}
