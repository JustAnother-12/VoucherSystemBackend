package com.dacn.VoucherSystem.dto.response;

import com.dacn.VoucherSystem.enums.ConditionType;
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
        "conditionType",
        "status",
        "config"
})
public record ConditionResponse(
        Long id,
        String name,
        ConditionType conditionType,
        @Schema(type = "object", example = "{}")
        JsonNode config,
        RuleStatus status
) {
}
