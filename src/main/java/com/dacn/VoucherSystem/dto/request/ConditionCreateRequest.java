package com.dacn.VoucherSystem.dto.request;

import com.dacn.VoucherSystem.enums.ConditionType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import tools.jackson.databind.JsonNode;

@Data
public class ConditionCreateRequest {
    @NotBlank(message = "name must not be empty")
    @Size(max = 100)
    private String name;
    @NotNull
    private ConditionType conditionType;
    @NotNull
    @Schema(type = "object", example = "{}")
    private JsonNode config;
}
