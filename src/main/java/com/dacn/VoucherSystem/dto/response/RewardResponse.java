package com.dacn.VoucherSystem.dto.response;

import com.dacn.VoucherSystem.enums.RewardType;
import com.dacn.VoucherSystem.enums.RuleStatus;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Builder;
import lombok.Data;
import tools.jackson.databind.JsonNode;

@Data
@Builder
@JsonPropertyOrder({
        "id",
        "name",
        "rewardType",
        "status",
        "config"
})
public class RewardResponse {
    private Long id;
    private String name;
    private RewardType rewardType;
    private JsonNode config;
    private RuleStatus status;
}
