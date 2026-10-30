package com.dacn.VoucherSystem.service;

import com.dacn.VoucherSystem.entity.BlackList;
import com.dacn.VoucherSystem.entity.RateLimitConfig;
import com.dacn.VoucherSystem.enums.ActionOnExceed;
import com.dacn.VoucherSystem.enums.BlacklistType;
import com.dacn.VoucherSystem.enums.LimitScope;
import com.dacn.VoucherSystem.exception.RateLimitExceededException;
import com.dacn.VoucherSystem.exception.RedemptionBlockedException;
import com.dacn.VoucherSystem.repository.BlackListRepository;
import com.dacn.VoucherSystem.repository.RateLimitConfigRepository;
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
    private final RateLimitConfigRepository configRepository;
    private final BlackListRepository blackListRepository;
    private final Clock clock;

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    private static final DefaultRedisScript<List> INCREMENT_SCRIPT;

    static {
        INCREMENT_SCRIPT = new DefaultRedisScript<>();

        INCREMENT_SCRIPT.setScriptText("""
            local count = redis.call('INCR', KEYS[1])

            if count == 1 then
                redis.call('EXPIRE', KEYS[1], ARGV[1])
            end

            return {count}
            """);

        INCREMENT_SCRIPT.setResultType(List.class);
    }

    public void check(
            String endpoint,
            String customerId,
            String ipAddress,
            String deviceFingerprint
    ) {
        List<RateLimitConfig> configs = configRepository.findByIsActiveTrueOrderByPriorityDesc();

        for (RateLimitConfig config : configs) {
            if (!pathMatcher.match(config.getEndpointPattern(), endpoint)) {
                continue;
            }

            String subject = resolveSubject(
                    config.getLimitScope(),
                    customerId,
                    ipAddress,
                    deviceFingerprint
            );

            if (!StringUtils.hasText(subject)) {
                continue;
            }

            String key = buildKey(config, subject);

            long requestCount = increment(
                    key,
                    config.getTimeWindowSeconds()
            );

            if (requestCount <= config.getMaxRequest()) {
                continue;
            }

            if (config.getActionOnExceed() == ActionOnExceed.BLACKLIST) {
                createTemporaryBlacklist(config, subject);

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
                INCREMENT_SCRIPT,
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
            RateLimitConfig config,
            String subject
    ) {
        return "rate-limit:"
                + config.getId()
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
            RateLimitConfig config,
            String subject
    ) {
        BlacklistType type = switch (config.getLimitScope()) {
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

        if (alreadyBlocked) {
            return;
        }

        BlackList blackList = BlackList.builder()
                .blacklistType(type)
                .targetValue(subject)
                .reason("Automatically blocked by rate limiter")
                .isActive(true)
                .expiredAt(now.plusSeconds(config.getTimeWindowSeconds()))
                .build();

        blackListRepository.save(blackList);
    }
}
