package com.dacn.VoucherSystem.cache;

import com.dacn.VoucherSystem.entity.RateLimitConfig;
import com.dacn.VoucherSystem.exception.RedemptionUnavailableException;
import com.dacn.VoucherSystem.repository.RateLimitConfigRepository;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
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

    public static final String CACHE_KEY =
            "rate-limit:config:active";

    public static final String INVALIDATION_CHANNEL =
            "cache:invalidate:rate-limit-policy";

    private static final Duration REDIS_TTL =
            Duration.ofSeconds(60);

    private static final Duration LOCAL_TTL =
            Duration.ofSeconds(10);

    private final Cache<String, List<RateLimitPolicy>> localCache =
            Caffeine.newBuilder()
                    .maximumSize(1)
                    .expireAfterWrite(LOCAL_TTL)
                    .recordStats()
                    .build();


    private final StringRedisTemplate redis;
    private final RateLimitConfigRepository repository;
    private final ObjectMapper objectMapper;

    public List<RateLimitPolicy> getActivePolicies() {
        return localCache.get(
                CACHE_KEY,
                this::loadPolicies
        );
    }

    public void warmUp() {
        List<RateLimitPolicy> immutablePolicies = List.copyOf(getPoliciesFromDatabase());
        try {
            // Populate Redis L2 first.
            redis.opsForValue().set(
                    CACHE_KEY,
                    objectMapper.writeValueAsString(immutablePolicies),
                    REDIS_TTL
            );

            // Populate Caffeine L1.
            localCache.put(
                    CACHE_KEY,
                    immutablePolicies
            );

            log.info(
                    "Rate limit policy cache warmed with {} policies",
                    immutablePolicies.size()
            );
        } catch (DataAccessException ex) {
            throw new RedemptionUnavailableException(
                    "Could not initialize rate limit policy cache",
                    ex
            );
        } catch (Exception ex) {
            throw new IllegalStateException(
                    "Could not serialize rate limit policies",
                    ex
            );
        }
    }

    private List<RateLimitPolicy> getPoliciesFromDatabase(){
        return repository.findByIsActiveTrueOrderByPriorityDesc()
                .stream()
                .map(this::toPolicy)
                .toList();
    }

    private List<RateLimitPolicy> loadPolicies(String key) {
        String cached;
        try {
            cached = redis.opsForValue().get(CACHE_KEY);

        } catch (DataAccessException ex) {
            throw new RedemptionUnavailableException(
                    "Rate limit configuration cache is unavailable",
                    ex
            );
        }

        // Try Redis L2.
        if (cached != null) {
            try {
                List<RateLimitPolicy> policies = objectMapper.readValue(
                        cached,
                        new TypeReference<List<RateLimitPolicy>>() {}
                );

                return List.copyOf(policies);

            } catch (Exception ex) {
                log.warn(
                        "Invalid rate limit config cache. Rebuilding it.",
                        ex
                );

                try {
                    redis.delete(CACHE_KEY);

                } catch (DataAccessException redisEx) {
                    throw new RedemptionUnavailableException(
                            "Could not remove invalid rate limit cache",
                            redisEx
                    );
                }
            }
        }

        // Redis miss: load from PostgreSQL.
        List<RateLimitPolicy> immutablePolicies = List.copyOf(getPoliciesFromDatabase());

        // Refresh Redis L2.
        try {
            redis.opsForValue().set(
                    CACHE_KEY,
                    objectMapper.writeValueAsString(immutablePolicies),
                    REDIS_TTL
            );
        } catch (DataAccessException ex) {
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

        return immutablePolicies;
    }

    /**
     * Called after admin updates rate limit configuration.
     */
    public void invalidate() {
        // Invalidate local instance first.
        invalidateLocal();

        // Invalidate shared Redis cache.
        try {
            redis.delete(CACHE_KEY);

        } catch (DataAccessException ex) {
            log.error(
                    "Could not invalidate Redis rate limit policy cache",
                    ex
            );
        }

        // Notify other application instances.
        try {
            redis.convertAndSend(
                    INVALIDATION_CHANNEL,
                    "invalidate"
            );
        } catch (DataAccessException ex) {
            log.error(
                    "Could not publish rate limit cache invalidation",
                    ex
            );
        }
    }

    public void invalidateLocal() {
        localCache.invalidateAll();
    }

    public CacheStats getCacheStats() {
        return localCache.stats();
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
