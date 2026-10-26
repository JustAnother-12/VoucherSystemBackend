package com.dacn.VoucherSystem.dto.request;

import com.dacn.VoucherSystem.enums.RewardType;
import lombok.Data;
import org.openapitools.jackson.nullable.JsonNullable;
import tools.jackson.databind.JsonNode;

@Data
public class RewardUpdateRequest {
    private JsonNullable<String> name;
    private JsonNullable<RewardType> type;
    private JsonNullable<JsonNode> config;
}
