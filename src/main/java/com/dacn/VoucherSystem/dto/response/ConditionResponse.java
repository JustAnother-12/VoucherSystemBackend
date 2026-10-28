package com.dacn.VoucherSystem.dto.response;

import com.dacn.VoucherSystem.enums.ConditionType;
import com.dacn.VoucherSystem.enums.RuleStatus;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import tools.jackson.databind.JsonNode;

@Data
@Builder
@JsonPropertyOrder({
        "id",
        "name",
        "conditionType",
        "status",
        "config"
})
public class ConditionResponse {
    private Long id;
    private String name;
    private ConditionType conditionType;
    @Schema(type = "object", example = "{}")
    private JsonNode config;
    private RuleStatus status;
}
