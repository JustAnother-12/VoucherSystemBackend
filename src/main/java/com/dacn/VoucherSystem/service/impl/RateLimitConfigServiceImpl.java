package com.dacn.VoucherSystem.service.impl;

import com.dacn.VoucherSystem.dto.request.RateLimitConfigCreateRequest;
import com.dacn.VoucherSystem.dto.response.RateLimitConfigResponse;
import com.dacn.VoucherSystem.entity.RateLimitConfig;
import com.dacn.VoucherSystem.enums.ActionOnExceed;
import com.dacn.VoucherSystem.enums.LimitScope;
import com.dacn.VoucherSystem.exception.ResourceNotFoundException;
import com.dacn.VoucherSystem.mapper.RateLimitConfigMapper;
import com.dacn.VoucherSystem.repository.RateLimitConfigRepository;
import com.dacn.VoucherSystem.service.RateLimitConfigService;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RateLimitConfigServiceImpl implements RateLimitConfigService {
    private RateLimitConfigRepository repository;
    private RateLimitConfigMapper mapper;

    @Override
    public List<RateLimitConfigResponse> getAllRateLimitConfig() {
        List<RateLimitConfig> configs = repository.findAll();
        if(configs.isEmpty())
            throw new ResourceNotFoundException("No configs found in the system");

        return configs.stream()
                .map(mapper::toRateLimitConfigResponse)
                .toList();
    }

    @Override
    public RateLimitConfigResponse getRateLimitConfigById(Long id) {
        RateLimitConfig config = repository
                .findById(id)
                .orElseThrow(
                        () -> new ResourceNotFoundException("No config with id " + id + " found in the system")
                );

        return mapper.toRateLimitConfigResponse(config);
    }

    @Override
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
                        .name(name)
                        .endpointPattern(endpointPattern)
                        .limitScope(scope)
                        .actionOnExceed(actionOnExceed)
                        .maxRequest(maxRequest)
                        .timeWindowSeconds(timeWindowSeconds)
                        .priority(priority)
                        .build();
        config.setIsActive(true);
        config.setCreatedAt(LocalDateTime.now());

        repository.saveAndFlush(config);

        return mapper.toRateLimitConfigResponse(config);
    }

    @Override
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

        if (name.isPresent())
            config.setName(name.get());

        if (endpointPattern.isPresent())
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

        return mapper.toRateLimitConfigResponse(config);
    }
}
