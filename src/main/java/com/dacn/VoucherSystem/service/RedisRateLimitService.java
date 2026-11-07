package com.dacn.VoucherSystem.service;

import com.dacn.VoucherSystem.cache.BlackListStatusCache;
import com.dacn.VoucherSystem.cache.RateLimitPolicy;
import com.dacn.VoucherSystem.cache.RateLimitPolicyCache;
import com.dacn.VoucherSystem.entity.BlackList;
import com.dacn.VoucherSystem.enums.ActionOnExceed;
import com.dacn.VoucherSystem.enums.BlacklistType;
import com.dacn.VoucherSystem.enums.LimitScope;
import com.dacn.VoucherSystem.exception.RateLimitExceededException;
import com.dacn.VoucherSystem.exception.RedemptionBlockedException;
import com.dacn.VoucherSystem.exception.RedemptionUnavailableException;
import com.dacn.VoucherSystem.repository.BlackListRepository;
import com.dacn.VoucherSystem.repository.RateLimitConfigRepository;
import com.dacn.VoucherSystem.service.interfaces.FraudLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RedisRateLimitService {
    private final StringRedisTemplate redis;
    private final BlackListRepository blackListRepository;
    private final Clock clock;

    private final RateLimitPolicyCache policyCache;
    private final FraudLogService fraudLogService;
    private final BlackListStatusCache blackListStatusCache;

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    private static final DefaultRedisScript<List> INCREMENT_MANY_SCRIPT;

    static {
        INCREMENT_MANY_SCRIPT = new DefaultRedisScript<>();

        INCREMENT_MANY_SCRIPT.setScriptText("""
        local results = {}

        for i = 1, #KEYS do
            local count = redis.call('INCR', KEYS[i])

            if count == 1 then
                redis.call('EXPIRE', KEYS[i], ARGV[i])
            end

            results[i] = count
        end

        return results
        """);

        INCREMENT_MANY_SCRIPT.setResultType(List.class);
    }

    private record ApplicablePolicy(
            RateLimitPolicy policy,
            String subject,
            String redisKey
    ) {
    }

    public void check(
            String endpoint,
            String customerId,
            String ipAddress,
            String deviceFingerprint
    ) {
        List<ApplicablePolicy> matchedPolicies =
                policyCache.getActivePolicies()
                        .stream()
                        .filter(policy ->
                                pathMatcher.match(
                                        policy.endpointPattern(),
                                        endpoint
                                )
                        )
                        .map(policy -> {
                            String subject = resolveSubject(
                                    policy.limitScope(),
                                    customerId,
                                    ipAddress,
                                    deviceFingerprint
                            );

                            if (!StringUtils.hasText(subject)) {
                                return null;
                            }

                            return new ApplicablePolicy(
                                    policy,
                                    subject,
                                    buildKey(policy, subject)
                            );
                        })
                        .filter(java.util.Objects::nonNull)
                        .toList();

        if (matchedPolicies.isEmpty()) {
            return;
        }

        List<String> keys = matchedPolicies.stream()
                .map(ApplicablePolicy::redisKey)
                .toList();

        Object[] windows = matchedPolicies.stream()
                .map(item -> item.policy().timeWindowSeconds().toString())
                .toArray();

        List<?> counts = redis.execute(
                INCREMENT_MANY_SCRIPT,
                keys,
                windows
        );

        if (counts == null || counts.size() != matchedPolicies.size()) {
            throw new RedemptionUnavailableException(
                    "Could not execute rate limit counters",
                    null
            );
        }

        // Policies đã được repository sắp xếp priority DESC.
        for (int i = 0; i < matchedPolicies.size(); i++) {

            ApplicablePolicy item = matchedPolicies.get(i);

            long count = Long.parseLong(
                    counts.get(i).toString()
            );

            if (count <= item.policy().maxRequest()) {
                continue;
            }

            if (item.policy().actionOnExceed() == ActionOnExceed.BLACKLIST) {

                createTemporaryBlacklist(
                        item.policy(),
                        item.subject(),
                        customerId,
                        ipAddress,
                        deviceFingerprint
                );

                throw new RedemptionBlockedException(
                        "Too many requests. This subject has been temporarily blocked."
                );
            }

            throw new RateLimitExceededException(
                    "Rate limit exceeded. Please try again later."
            );
        }
    }

    private long increment(String key, int windowSeconds) {
        List<?> result = redis.execute(
                INCREMENT_MANY_SCRIPT,
                List.of(key),
                String.valueOf(windowSeconds)
        );

        if (result == null || result.isEmpty()) {
            throw new IllegalStateException(
                    "Could not execute Redis rate limiter"
            );
        }

        return Long.parseLong(result.get(0).toString());
    }

    private String resolveSubject(
            LimitScope scope,
            String customerId,
            String ipAddress,
            String deviceFingerprint
    ) {
        return switch (scope) {
            case CUSTOMER_ID -> customerId;
            case IP -> ipAddress;
            case DEVICE_FINGERPRINT -> deviceFingerprint;
        };
    }

    private String buildKey(
            RateLimitPolicy config,
            String subject
    ) {
        return "rate-limit:counter:"
                + config.id()
                + ":"
                + config.maxRequest()
                + ":"
                + config.timeWindowSeconds()
                + ":"
                + sha256(subject);
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(
                    value.getBytes(StandardCharsets.UTF_8)
            );

            return HexFormat.of().formatHex(hash);

        } catch (Exception ex) {
            throw new IllegalStateException(
                    "Could not hash rate limit key",
                    ex
            );
        }
    }

    private void createTemporaryBlacklist(
            RateLimitPolicy config,
            String subject,
            String customerId,
            String ipAddress,
            String deviceFingerprint
    ) {
        BlacklistType type = switch (config.limitScope()) {
            case CUSTOMER_ID -> BlacklistType.CUSTOMER;
            case IP -> BlacklistType.IP;
            case DEVICE_FINGERPRINT -> BlacklistType.DEVICE_FINGERPRINT;
        };

        LocalDateTime now = LocalDateTime.now(clock);

        boolean alreadyBlocked = blackListRepository.existsActive(
                type,
                subject,
                now
        );

        if (!alreadyBlocked) {
            BlackList blackList = BlackList.builder()
                    .createdAt(LocalDateTime.now(clock))
                    .blacklistType(type)
                    .targetValue(subject)
                    .reason("Automatically blocked by rate limiter")
                    .isActive(true)
                    .expiredAt(now.plusSeconds(
                            config.timeWindowSeconds()
                    ))
                    .build();

            blackListRepository.saveAndFlush(blackList);

            blackListStatusCache.evict(type, subject);
        }

        fraudLogService.recordAutomatedFraud(
                null,
                customerId,
                ipAddress,
                deviceFingerprint,
                "EXCEEDED_RATE_LIMIT"
        );
    }
}
