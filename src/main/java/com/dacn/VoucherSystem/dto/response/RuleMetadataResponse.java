package com.dacn.VoucherSystem.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.List;

@JsonPropertyOrder({
        "schemaVersion",
        "conditionTypes",
        "rewardTypes"
})
public record RuleMetadataResponse(
        int schemaVersion,
        List<RuleTypeMetadata> conditionTypes,
        List<RuleTypeMetadata> rewardTypes
) {}
