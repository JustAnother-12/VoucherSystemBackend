package com.dacn.VoucherSystem.dto.request;

import org.openapitools.jackson.nullable.JsonNullable;

public record BatchRuleUpdateRequest(
        JsonNullable<Long> rewardId,
        JsonNullable<Long> conditionId
) {}
