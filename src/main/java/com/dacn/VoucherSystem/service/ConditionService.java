package com.dacn.VoucherSystem.service;

import com.dacn.VoucherSystem.dto.response.ConditionResponse;
import com.dacn.VoucherSystem.enums.ConditionType;
import org.openapitools.jackson.nullable.JsonNullable;
import tools.jackson.databind.JsonNode;

import java.util.List;

public interface ConditionService {
    List<ConditionResponse> getAllCondition();
    ConditionResponse getConditionById(Long id);
    ConditionResponse createCondition(
            String name,
            ConditionType type,
            JsonNode config
    );
    ConditionResponse updateCondition(
            Long id,
            JsonNullable<String> name,
            JsonNullable<ConditionType> type,
            JsonNullable<JsonNode> config
    );
}
