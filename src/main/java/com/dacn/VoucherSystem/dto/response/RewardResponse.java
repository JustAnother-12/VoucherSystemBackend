package com.dacn.VoucherSystem.dto.response;

import com.dacn.VoucherSystem.enums.RewardType;
import com.dacn.VoucherSystem.enums.RuleStatus;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import tools.jackson.databind.JsonNode;

@Builder
@JsonPropertyOrder({
        "id",
        "name",
        "rewardType",
        "status",
        "config"
})
public record RewardResponse(
        Long id,
        String name,
        RewardType rewardType,
        @Schema(type = "object", example = "{}")
        JsonNode config,
        RuleStatus status
) {
}
