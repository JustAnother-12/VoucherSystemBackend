package com.dacn.VoucherSystem.cache;

import com.dacn.VoucherSystem.entity.BlackList;
import com.dacn.VoucherSystem.enums.BlacklistType;
import com.dacn.VoucherSystem.exception.RedemptionUnavailableException;
import com.dacn.VoucherSystem.repository.BlackListRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class BlackListStatusCache {

    private static final Duration POSITIVE_TTL =
            Duration.ofSeconds(5);

    private static final Duration NEGATIVE_TTL =
            Duration.ofSeconds(3);

    private final StringRedisTemplate redis;
    private final BlackListRepository repository;
    private final Clock clock;

    public Set<BlacklistType> findBlockedTypes(
            Map<BlacklistType, String> subjects
    ) {
        Map<BlacklistType, String> validSubjects = new EnumMap<>(
                BlacklistType.class
        );

        subjects.forEach((type, value) -> {
            if (value != null && !value.isBlank()) {
                validSubjects.put(type, value.trim());
            }
        });

        if (validSubjects.isEmpty()) {
            return Set.of();
        }

        List<Map.Entry<BlacklistType, String>> entries =
                new ArrayList<>(validSubjects.entrySet());

        List<String> keys = entries.stream()
                .map(entry -> buildKey(entry.getKey(), entry.getValue()))
                .toList();

        List<String> cachedValues;

        try {
            cachedValues = redis.opsForValue().multiGet(keys);

        } catch (RedisConnectionFailureException ex) {
            throw new RedemptionUnavailableException(
                    "Blacklist cache is unavailable",
                    ex
            );
        }

        Set<BlacklistType> blocked = EnumSet.noneOf(
                BlacklistType.class
        );

        List<Map.Entry<BlacklistType, String>> misses =
                new ArrayList<>();

        for (int i = 0; i < entries.size(); i++) {

            Map.Entry<BlacklistType, String> entry = entries.get(i);
            String cached = cachedValues == null
                    ? null
                    : cachedValues.get(i);

            if (cached == null) {
                misses.add(entry);
                continue;
            }

            if ("1".equals(cached)) {
                blocked.add(entry.getKey());
            }
        }

        if (misses.isEmpty()) {
            return blocked;
        }

        Map<BlacklistType, String> missingSubjects =
                new EnumMap<>(BlacklistType.class);

        misses.forEach(entry ->
                missingSubjects.put(entry.getKey(), entry.getValue())
        );

        LocalDateTime now = LocalDateTime.now(clock);

        List<BlackList> databaseMatches = repository.findActiveMatches(
                BlacklistType.CUSTOMER,
                missingSubjects.get(BlacklistType.CUSTOMER),
                BlacklistType.IP,
                missingSubjects.get(BlacklistType.IP),
                BlacklistType.DEVICE_FINGERPRINT,
                missingSubjects.get(BlacklistType.DEVICE_FINGERPRINT),
                now
        );

        Set<BlacklistType> matchedTypes = EnumSet.noneOf(
                BlacklistType.class
        );

        databaseMatches.forEach(match -> {
            matchedTypes.add(match.getBlacklistType());
            blocked.add(match.getBlacklistType());
        });

        Map<BlacklistType, BlackList> matchByType =
                new EnumMap<>(BlacklistType.class);

        databaseMatches.forEach(match ->
                matchByType.put(match.getBlacklistType(), match)
        );

        for (Map.Entry<BlacklistType, String> entry : misses) {

            boolean isBlocked = matchedTypes.contains(entry.getKey());

            Duration ttl = isBlocked
                    ? calculatePositiveTtl(
                    matchByType.get(entry.getKey()),
                    now
            )
                    : NEGATIVE_TTL;

            try {
                redis.opsForValue().set(
                        buildKey(entry.getKey(), entry.getValue()),
                        isBlocked ? "1" : "0",
                        ttl
                );

            } catch (RedisConnectionFailureException ex) {
                throw new RedemptionUnavailableException(
                        "Could not cache blacklist status",
                        ex
                );
            }
        }

        return blocked;
    }

    public void evict(
            BlacklistType type,
            String targetValue
    ) {
        if (targetValue == null || targetValue.isBlank()) {
            return;
        }

        try {
            redis.delete(buildKey(type, targetValue.trim()));

        } catch (RedisConnectionFailureException ex) {

        }
    }

    private Duration calculatePositiveTtl(
            BlackList blacklist,
            LocalDateTime now
    ) {
        if (blacklist == null || blacklist.getExpiredAt() == null) {
            return POSITIVE_TTL;
        }

        Duration remaining = Duration.between(
                now,
                blacklist.getExpiredAt()
        );

        if (remaining.isNegative() || remaining.isZero()) {
            return Duration.ofMillis(100);
        }

        return remaining.compareTo(POSITIVE_TTL) < 0
                ? remaining
                : POSITIVE_TTL;
    }

    private String buildKey(
            BlacklistType type,
            String value
    ) {
        return "blacklist:status:"
                + type.name()
                + ":"
                + sha256(value);
    }

    private String sha256(String value) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));

            return HexFormat.of().formatHex(hash);

        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }
}
