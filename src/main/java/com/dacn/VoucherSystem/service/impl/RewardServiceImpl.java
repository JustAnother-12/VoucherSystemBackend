package com.dacn.VoucherSystem.service.impl;

import com.dacn.VoucherSystem.dto.response.RewardResponse;
import com.dacn.VoucherSystem.entity.Reward;
import com.dacn.VoucherSystem.enums.RewardType;
import com.dacn.VoucherSystem.enums.RuleStatus;
import com.dacn.VoucherSystem.event.VoucherSnapshotsChangedEvent;
import com.dacn.VoucherSystem.exception.ResourceNotFoundException;
import com.dacn.VoucherSystem.exception.RuleConfigHasPublishedException;
import com.dacn.VoucherSystem.mapper.RewardMapper;
import com.dacn.VoucherSystem.repository.RewardRepository;
import com.dacn.VoucherSystem.service.interfaces.RewardService;
import com.dacn.VoucherSystem.util.RuleConfigValidator;
import lombok.RequiredArgsConstructor;
import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;

import java.util.List;
import java.time.Clock;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class RewardServiceImpl implements RewardService {
    private final ApplicationEventPublisher eventPublisher;
    private final RewardRepository repository;
    private final RewardMapper mapper;
    private final RuleConfigValidator validator;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public List<RewardResponse> getAllReward() {
        List<Reward> reward = repository.findAll();

        return reward.stream()
                .map(mapper::toRewardResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public RewardResponse getRewardById(Long id) {
        Reward reward = repository
                .findById(id)
                .orElseThrow(
                        () -> new ResourceNotFoundException("No reward with id " + id + " found in the system")
                );

        return mapper.toRewardResponse(reward);
    }

    @Override
    @Transactional
    public RewardResponse createReward(
            String name,
            RewardType type,
            JsonNode config
    ) {

        validator.validateReward(type, config);

        Reward reward =
                Reward.builder()
                        .createdAt(LocalDateTime.now(clock))
                        .rewardName(name)
                        .rewardType(type)
                        .rewardJson(config)
                        .build();

        reward.setStatus(RuleStatus.DRAFT);

        repository.saveAndFlush(reward);

        return mapper.toRewardResponse(reward);
    }

    @Override
    @Transactional
    public RewardResponse updateReward(
            Long id,
            JsonNullable<String> name,
            JsonNullable<RewardType> type,
            JsonNullable<JsonNode> config
    ) {
        Reward reward = repository
                .findById(id)
                .orElseThrow(
                        () -> new ResourceNotFoundException("No reward with id " + id + " found in the system")
                );

        if (reward.getStatus().equals(RuleStatus.PUBLISHED))
            throw new RuleConfigHasPublishedException("The reward has been Published");

        if (name.isPresent() && !name.get().isEmpty())
            reward.setRewardName(name.get());

        RewardType updatedType = type.isPresent()
                ? type.get()
                : reward.getRewardType();

        JsonNode updatedConfig = config.isPresent()
                ? config.get()
                : reward.getRewardJson();

        validator.validateReward(updatedType, updatedConfig);

        reward.setRewardType(updatedType);
        reward.setRewardJson(updatedConfig);

        repository.save(reward);

        eventPublisher.publishEvent(
                new VoucherSnapshotsChangedEvent()
        );

        return mapper.toRewardResponse(reward);
    }

}
