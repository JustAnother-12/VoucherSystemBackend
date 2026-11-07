package com.dacn.VoucherSystem.service.impl;

import com.dacn.VoucherSystem.dto.response.RateLimitConfigResponse;
import com.dacn.VoucherSystem.entity.RateLimitConfig;
import com.dacn.VoucherSystem.enums.ActionOnExceed;
import com.dacn.VoucherSystem.enums.LimitScope;
import com.dacn.VoucherSystem.event.RateLimitConfigsChangedEvent;
import com.dacn.VoucherSystem.exception.ResourceNotFoundException;
import com.dacn.VoucherSystem.mapper.RateLimitConfigMapper;
import com.dacn.VoucherSystem.repository.RateLimitConfigRepository;
import com.dacn.VoucherSystem.service.interfaces.RateLimitConfigService;
import lombok.RequiredArgsConstructor;
import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.time.Clock;

@Service
@RequiredArgsConstructor
public class RateLimitConfigServiceImpl implements RateLimitConfigService {
    private final ApplicationEventPublisher eventPublisher;
    private final RateLimitConfigRepository repository;
    private final RateLimitConfigMapper mapper;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public List<RateLimitConfigResponse> getAllRateLimitConfig() {
        List<RateLimitConfig> configs = repository.findAll();

        return configs.stream()
                .map(mapper::toRateLimitConfigResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public RateLimitConfigResponse getRateLimitConfigById(Long id) {
        RateLimitConfig config = repository
                .findById(id)
                .orElseThrow(
                        () -> new ResourceNotFoundException("No config with id " + id + " found in the system")
                );

        return mapper.toRateLimitConfigResponse(config);
    }

    @Override
    @Transactional
    public RateLimitConfigResponse createRateLimitConfig(
            String name,
            String endpointPattern,
            LimitScope scope,
            Integer maxRequest,
            Integer timeWindowSeconds,
            ActionOnExceed actionOnExceed,
            Integer priority
    ){
        RateLimitConfig config =
                RateLimitConfig.builder()
                        .rateLimitConfigName(name)
                        .endpointPattern(endpointPattern)
                        .limitScope(scope)
                        .actionOnExceed(actionOnExceed)
                        .maxRequest(maxRequest)
                        .timeWindowSeconds(timeWindowSeconds)
                        .priority(priority)
                        .build();
        config.setIsActive(true);
        config.setCreatedAt(LocalDateTime.now(clock));

        repository.saveAndFlush(config);

        eventPublisher.publishEvent(
                new RateLimitConfigsChangedEvent()
        );

        return mapper.toRateLimitConfigResponse(config);
    }

    @Override
    @Transactional
    public RateLimitConfigResponse updateRateLimitConfig(
            Long id,
            JsonNullable<String> name,
            JsonNullable<String> endpointPattern,
            JsonNullable<LimitScope> limitScope,
            JsonNullable<Integer> maxRequest,
            JsonNullable<Integer> timeWindowSeconds,
            JsonNullable<ActionOnExceed> actionOnExceed,
            JsonNullable<Integer> priority,
            JsonNullable<Boolean> isActive
    ) {
        RateLimitConfig config = repository
                .findById(id)
                .orElseThrow(
                        () -> new ResourceNotFoundException("No config with id " + id + " found in the system")
                );

        if (name.isPresent() && !name.get().isEmpty())
            config.setRateLimitConfigName(name.get());

        if (endpointPattern.isPresent() && !endpointPattern.get().isEmpty())
            config.setEndpointPattern(endpointPattern.get());

        if (limitScope.isPresent())
            config.setLimitScope(limitScope.get());

        if (maxRequest.isPresent())
            config.setMaxRequest(maxRequest.get());

        if (timeWindowSeconds.isPresent())
            config.setTimeWindowSeconds(timeWindowSeconds.get());

        if (actionOnExceed.isPresent())
            config.setActionOnExceed(actionOnExceed.get());

        if (priority.isPresent())
            config.setPriority(priority.get());

        if (isActive.isPresent())
            config.setIsActive(isActive.get());

        repository.save(config);

        eventPublisher.publishEvent(
                new RateLimitConfigsChangedEvent()
        );

        return mapper.toRateLimitConfigResponse(config);
    }
}
