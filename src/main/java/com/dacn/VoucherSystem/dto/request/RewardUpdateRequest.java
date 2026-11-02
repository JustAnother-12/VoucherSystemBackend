package com.dacn.VoucherSystem.dto.request;

import com.dacn.VoucherSystem.enums.RewardType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.openapitools.jackson.nullable.JsonNullable;
import tools.jackson.databind.JsonNode;

public record RewardUpdateRequest(
        JsonNullable<String> name,
        JsonNullable<RewardType> rewardType,
        @Schema(type = "object", example = "{}")
        JsonNullable<JsonNode> config
) {
}
