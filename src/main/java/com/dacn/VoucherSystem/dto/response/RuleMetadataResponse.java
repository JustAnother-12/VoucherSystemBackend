package com.dacn.VoucherSystem.dto.response;

import java.util.List;

public record RuleMetadataResponse(
        int schemaVersion,
        List<RuleTypeMetadata> conditionTypes,
        List<RuleTypeMetadata> rewardTypes
) {}
