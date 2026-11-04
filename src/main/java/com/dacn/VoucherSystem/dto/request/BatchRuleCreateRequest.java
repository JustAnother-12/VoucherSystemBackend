package com.dacn.VoucherSystem.dto.request;

import jakarta.validation.constraints.NotNull;

public record BatchRuleCreateRequest(
        @NotNull Long rewardId,
        @NotNull Long conditionId
) {}