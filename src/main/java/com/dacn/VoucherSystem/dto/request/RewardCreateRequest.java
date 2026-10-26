package com.dacn.VoucherSystem.dto.request;

import com.dacn.VoucherSystem.enums.RewardType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import tools.jackson.databind.JsonNode;

@Data
public class RewardCreateRequest {
    @NotBlank(message = "name must not be empty")
    @Size(max = 100)
    private String name;
    @NotNull
    private RewardType type;
    @NotNull
    private JsonNode config;
}
