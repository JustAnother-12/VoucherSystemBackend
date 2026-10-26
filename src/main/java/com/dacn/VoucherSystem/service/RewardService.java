package com.dacn.VoucherSystem.service;

import com.dacn.VoucherSystem.dto.response.RewardResponse;
import com.dacn.VoucherSystem.dto.response.RewardResponse;
import com.dacn.VoucherSystem.enums.RewardType;
import org.openapitools.jackson.nullable.JsonNullable;
import tools.jackson.databind.JsonNode;

import java.util.List;

public interface RewardService {
    List<RewardResponse> getAllReward();
    RewardResponse getRewardById(Long id);
    RewardResponse createReward(
            String name,
            RewardType type,
            JsonNode config
    );
    RewardResponse updateReward(
            Long id,
            JsonNullable<String> name,
            JsonNullable<RewardType> type,
            JsonNullable<JsonNode> config
    );
}
