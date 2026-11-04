package com.dacn.VoucherSystem.dto.response;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({
        "id",
        "rewardId",
        "conditionId"
})
public record BatchRuleResponse(
        Long id,
        Long rewardId,
        Long conditionId
) {}
