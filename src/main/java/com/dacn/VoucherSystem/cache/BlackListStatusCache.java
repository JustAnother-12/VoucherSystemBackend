package com.dacn.VoucherSystem.cache;

import com.dacn.VoucherSystem.entity.BlackList;
import com.dacn.VoucherSystem.enums.BlacklistType;
import com.dacn.VoucherSystem.exception.RedemptionUnavailableException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.DefaultTypedTuple;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.*;

import static com.dacn.VoucherSystem.util.Helpers.sha256;

@Service
@RequiredArgsConstructor
public class BlackListStatusCache {

    private static final String INDEX_KEY =
            "blacklist:active:index";

    /*
     * Redis scores are double values.
     * This value is sufficiently large and exactly representable.
     * It represents a blacklist without expiration.
     */
    private static final double PERMANENT_SCORE =
            9_007_199_254_740_991D;

    private static final DefaultRedisScript<List> LOOKUP_SCRIPT;

    static {
        LOOKUP_SCRIPT = new DefaultRedisScript<>();

        LOOKUP_SCRIPT.setScriptText("""
            local now = tonumber(ARGV[#ARGV])
            local blocked = {}

            for i = 1, #ARGV - 1 do
                local member = ARGV[i]

                local score = redis.call(
                    'ZSCORE',
                    KEYS[1],
                    member
                )

                if score and tonumber(score) > now then
                    table.insert(blocked, i)

                elseif score then
                    redis.call(
                        'ZREM',
                        KEYS[1],
                        member
                    )
                end
            end

            return blocked
            """);

        LOOKUP_SCRIPT.setResultType(List.class);
    }

    private final StringRedisTemplate redis;
    private final Clock clock;

    /**
     * Check blacklist membership for all supplied subjects.
     * This method never queries PostgreSQL.
     */
    public Set<BlacklistType> findBlockedTypes(
            Map<BlacklistType, String> subjects
    ) {
        if (subjects == null || subjects.isEmpty()) {
            return Set.of();
        }

        List<Map.Entry<BlacklistType, String>> entries = subjects.entrySet()
                .stream()
                .filter(entry ->
                                entry.getValue() != null && !entry.getValue().isBlank()
                )
                .map(entry ->
                        Map.entry(entry.getKey(), entry.getValue().trim())
                ).toList();

        if (entries.isEmpty()) {
            return Set.of();
        }

        List<Object> arguments = new ArrayList<>();

        for (Map.Entry<BlacklistType, String> entry : entries) {
            arguments.add(
                    buildMember(
                            entry.getKey(),
                            entry.getValue()
                    )
            );
        }

        arguments.add(String.valueOf(clock.millis()));

        try {
            List<?> results = redis.execute(
                    LOOKUP_SCRIPT,
                    List.of(INDEX_KEY),
                    arguments.toArray()
            );

            if (results == null) {
                throw new RedemptionUnavailableException(
                        "Could not check blacklist index",
                        null
                );
            }

            Set<BlacklistType> blocked = EnumSet.noneOf(BlacklistType.class);

            for (Object result : results) {
                int index = Integer.parseInt(
                        result.toString()
                );

                if (index >= 1 && index <= entries.size()) {
                    blocked.add(
                            entries.get(index - 1).getKey()
                    );
                }
            }

            return blocked;

        } catch (DataAccessException ex) {
            throw new RedemptionUnavailableException(
                    "Blacklist index is unavailable",
                    ex
            );
        }
    }

    /**
     * Add or update a blacklist entry in Redis.
     */
    public void upsert(
            BlacklistType type,
            String targetValue,
            LocalDateTime expiredAt
    ) {
        if (targetValue == null || targetValue.isBlank()) {
            return;
        }

        String member = buildMember(type, targetValue.trim());

        double score = expiredAt == null ? PERMANENT_SCORE :
                expiredAt.atZone(clock.getZone())
                .toInstant()
                .toEpochMilli();

        try {
            if (score <= clock.millis()) {
                redis.opsForZSet().remove(
                        INDEX_KEY,
                        member
                );

                return;
            }

            redis.opsForZSet().add(
                    INDEX_KEY,
                    member,
                    score
            );

        } catch (DataAccessException ex) {
            throw new RedemptionUnavailableException(
                    "Could not update blacklist index",
                    ex
            );
        }
    }

    /**
     * Remove a blacklist entry from Redis.
     */
    public void remove(
            BlacklistType type,
            String targetValue
    ) {
        if (targetValue == null || targetValue.isBlank()) {
            return;
        }

        try {
            redis.opsForZSet().remove(
                    INDEX_KEY,
                    buildMember(type, targetValue.trim())
            );

        } catch (DataAccessException ex) {
            throw new RedemptionUnavailableException(
                    "Could not remove blacklist index entry",
                    ex
            );
        }
    }

    /**
     * Populate the Redis index from active database records.
     * Run this during application startup.
     */
    public void warmUp(Collection<BlackList> records) {
        if (records == null || records.isEmpty()) {
            return;
        }

        try {
            long now = clock.millis();

            Set<org.springframework.data.redis.core.ZSetOperations
                    .TypedTuple<String>> tuples = new HashSet<>();

            for (BlackList record : records) {

                if (!Boolean.TRUE.equals(record.getIsActive())) {
                    continue;
                }

                double score = record.getExpiredAt() == null ? PERMANENT_SCORE :
                        record.getExpiredAt()
                        .atZone(clock.getZone())
                        .toInstant()
                        .toEpochMilli();

                if (score <= now) {
                    continue;
                }

                String member = buildMember(
                        record.getBlacklistType(),
                        record.getTargetValue()
                );

                tuples.add(
                        new DefaultTypedTuple<>(
                                member,
                                score
                        )
                );
            }

            if (!tuples.isEmpty()) {
                redis.opsForZSet().add(
                        INDEX_KEY,
                        tuples
                );
            }

        } catch (DataAccessException ex) {
            throw new RedemptionUnavailableException(
                    "Could not initialize blacklist index",
                    ex
            );
        }
    }

    private String buildMember(
            BlacklistType type,
            String value
    ) {
        return type.name() + ":" + sha256(value);
    }
}
