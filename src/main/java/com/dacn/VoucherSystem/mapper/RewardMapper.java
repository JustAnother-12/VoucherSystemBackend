package com.dacn.VoucherSystem.mapper;

import com.dacn.VoucherSystem.dto.response.RewardResponse;
import com.dacn.VoucherSystem.entity.Reward;
import org.springframework.stereotype.Component;

@Component
public class RewardMapper {
    public RewardResponse toRewardResponse(Reward reward) {
        if (reward == null) {
            return null;
        }
        return RewardResponse.builder()
                .id(reward.getId())
                .name(reward.getName())
                .rewardType(reward.getRewardType())
                .status(reward.getStatus())
                .config(reward.getRewardJson())
                .build();
    }
}
