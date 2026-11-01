package com.dacn.VoucherSystem.cache;

import com.dacn.VoucherSystem.entity.RateLimitConfig;
import com.dacn.VoucherSystem.exception.RedemptionUnavailableException;
import com.dacn.VoucherSystem.repository.RateLimitConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RateLimitPolicyCache {

    private static final String CACHE_KEY =
            "rate-limit:config:active";

    private static final Duration TTL =
            Duration.ofSeconds(60);

    private final StringRedisTemplate redis;
    private final RateLimitConfigRepository repository;
    private final ObjectMapper objectMapper;

    public List<RateLimitPolicy> getActivePolicies() {

        String cached;

        try {
            cached = redis.opsForValue().get(CACHE_KEY);

        } catch (RedisConnectionFailureException ex) {
            throw new RedemptionUnavailableException(
                    "Rate limit configuration cache is unavailable",
                    ex
            );
        }

        if (cached != null) {
            try {
                return objectMapper.readValue(
                        cached,
                        new TypeReference<List<RateLimitPolicy>>() {}
                );

            } catch (Exception ex) {
                log.warn("Invalid rate limit config cache, rebuilding it");
                redis.delete(CACHE_KEY);
            }
        }

        List<RateLimitPolicy> policies =
                repository.findByIsActiveTrueOrderByPriorityDesc()
                        .stream()
                        .map(this::toPolicy)
                        .toList();

        try {
            redis.opsForValue().set(
                    CACHE_KEY,
                    objectMapper.writeValueAsString(policies),
                    TTL
            );

        } catch (RedisConnectionFailureException ex) {
            throw new RedemptionUnavailableException(
                    "Could not cache rate limit configuration",
                    ex
            );

        } catch (Exception ex) {
            throw new IllegalStateException(
                    "Could not serialize rate limit configuration",
                    ex
            );
        }

        return policies;
    }

    public void invalidate() {
        try {
            redis.delete(CACHE_KEY);

        } catch (RedisConnectionFailureException ex) {
            log.error("Could not invalidate rate limit policy cache", ex);
        }
    }

    private RateLimitPolicy toPolicy(RateLimitConfig config) {
        return new RateLimitPolicy(
                config.getId(),
                config.getEndpointPattern(),
                config.getLimitScope(),
                config.getMaxRequest(),
                config.getTimeWindowSeconds(),
                config.getActionOnExceed(),
                config.getPriority()
        );
    }
}
