package com.dacn.VoucherSystem.cache;

import com.dacn.VoucherSystem.entity.*;
import com.dacn.VoucherSystem.exception.ResourceNotFoundException;
import com.dacn.VoucherSystem.repository.VoucherRepository;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.dao.DataAccessException;
import org.redisson.client.RedisException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import static com.dacn.VoucherSystem.util.Helpers.sha256;

@Slf4j
@Service
@RequiredArgsConstructor
public class VoucherSnapshotCache {

    private static final Duration REDIS_TTL =
            Duration.ofSeconds(10);

    private static final Duration LOCAL_TTL =
            Duration.ofSeconds(5);

    private static final String VERSION_KEY =
            "voucher:snapshot:version";

    public static final String INVALIDATION_CHANNEL =
            "cache:invalidate:voucher-snapshot";

    private final Cache<String, VoucherRuleSnapshot> localCache =
            Caffeine.newBuilder()
                    .maximumSize(10_000)
                    .expireAfterWrite(LOCAL_TTL)
                    .recordStats()
                    .build();

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final VoucherRepository voucherRepository;
    private final RedissonClient redisson;
    private final Clock clock;

    public VoucherRuleSnapshot getByCode(String rawCode) {

        String code = rawCode.trim()
                .toUpperCase(Locale.ROOT);

        return localCache.get(
                code,
                this::loadSnapshot
        );
    }

    /**
     * Load snapshot from local cache, Redis, or PostgreSQL.
     */
    private VoucherRuleSnapshot loadSnapshot(String code) {

        String version = getCacheVersion();

        /*
         * Redis unavailable:
         * Fall back to PostgreSQL.
         */
        if (version == null) {
            return loadFromDatabase(code);
        }

        String redisKey =
                "voucher:voucherCode:"
                        + code
                        + ":v"
                        + version;

        // First Redis lookup.
        VoucherRuleSnapshot cached = readSharedCache(redisKey);

        if (cached != null) {
            return cached;
        }

        /*
         * Distributed lock is used only for rebuilding
         * the snapshot cache.
         */
        String lockKey = "lock:voucher:snapshot:refresh:"
                + sha256(code)
                + ":v"
                + version;

        RLock lock = redisson.getLock(lockKey);

        boolean acquired = false;

        try {

            acquired = lock.tryLock(
                    500,
                    TimeUnit.MILLISECONDS
            );

            /*
             * Another application instance may have
             * rebuilt the snapshot while we were waiting.
             */
            if (!acquired) {

                cached = readSharedCache(redisKey);

                if (cached != null) {
                    return cached;
                }

                // Do not write L2 without holding refresh lock.
                return loadFromDatabase(code);
            }

            // Double-check after acquiring the lock.
            cached = readSharedCache(redisKey);

            if (cached != null) {
                return cached;
            }

            // Rebuild from PostgreSQL.
            VoucherRuleSnapshot snapshot = loadFromDatabase(code);

            writeSharedCache(redisKey, snapshot);

            return snapshot;

        } catch (InterruptedException ex) {

            Thread.currentThread().interrupt();

            log.warn(
                    "Interrupted while waiting for voucher snapshot lock: {}",
                    code
            );

            return loadFromDatabase(code);

        } catch (RedisException ex) {

            log.warn(
                    "Redisson unavailable during snapshot refresh. "
                            + "Falling back to PostgreSQL.",
                    ex
            );

            return loadFromDatabase(code);

        } finally {

            if (acquired) {
                try {
                    if (lock.isHeldByCurrentThread()) {
                        lock.unlock();
                    }
                } catch (RedisException ex) {
                    log.warn(
                            "Could not release voucher snapshot refresh lock",
                            ex
                    );
                }
            }
        }
    }

    private String getCacheVersion() {

        try {

            String version = redis.opsForValue().get(VERSION_KEY);

            if (version != null) {
                return version;
            }

            redis.opsForValue().setIfAbsent(
                    VERSION_KEY,
                    "1"
            );

            return redis.opsForValue().get(VERSION_KEY);

        } catch (DataAccessException ex) {

            log.warn(
                    "Redis unavailable while reading voucher cache version"
            );

            return null;
        }
    }

    private VoucherRuleSnapshot readSharedCache(String key) {
        String cached;
        try {
            cached = redis.opsForValue().get(key);
        } catch (DataAccessException ex) {
            log.warn(
                    "Redis unavailable while reading snapshot: {}",
                    key
            );
            return null;
        }

        if (cached == null) {
            return null;
        }

        try {
            return objectMapper.readValue(
                    cached,
                    VoucherRuleSnapshot.class
            );

        } catch (Exception ex) {
            log.warn(
                    "Invalid voucher snapshot cache: {}",
                    key,
                    ex
            );

            try {
                redis.delete(key);
            } catch (DataAccessException redisEx) {
                log.warn(
                        "Could not delete invalid snapshot: {}",
                        key,
                        redisEx
                );
            }

            return null;
        }
    }

    private void writeSharedCache(
            String key,
            VoucherRuleSnapshot snapshot
    ) {

        try {
            redis.opsForValue().set(
                    key,
                    objectMapper.writeValueAsString(snapshot),
                    REDIS_TTL
            );

        } catch (DataAccessException ex) {
            log.warn(
                    "Could not write voucher snapshot to Redis",
                    ex
            );

        } catch (Exception ex) {
            log.warn(
                    "Could not serialize voucher snapshot",
                    ex
            );
        }
    }

    private VoucherRuleSnapshot loadFromDatabase(String code) {
        Voucher voucher = voucherRepository
                .findVoucherByCode(code)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Voucher voucherCode not found"
                        )
                );

        VoucherBatch batch = voucher.getBatch();
        Campaign campaign = voucher.getCampaign();
        BatchRule rule = batch.getRule();
        Condition condition = rule.getCondition();
        Reward reward = rule.getReward();

        if (!campaign.getId().equals(batch.getCampaign().getId())) {
            throw new IllegalStateException(
                    "Voucher campaign does not match batch campaign"
            );
        }

        return new VoucherRuleSnapshot(
                voucher.getId(),
                voucher.getCode(),
                batch.getId(),
                batch.getBatchType(),
                batch.getStatus(),
                batch.getGeneratedCount(),
                campaign.getId(),
                campaign.getStatus(),
                campaign.getStartTime(),
                campaign.getEndTime(),
                condition.getConditionType(),
                condition.getStatus(),
                condition.getConditionJson(),
                reward.getRewardType(),
                reward.getStatus(),
                reward.getRewardJson()
        );
    }

    /**
     * Invalidate local and shared snapshots after a database commit.
     */
    public void invalidateAllSnapshots() {
        invalidateLocal();
        try {
            redis.opsForValue().increment(VERSION_KEY);
        } catch (DataAccessException ex) {
            log.warn(
                    "Could not increment voucher snapshot version",
                    ex
            );
            return;
        }

        try {
            redis.convertAndSend(
                    INVALIDATION_CHANNEL,
                    "invalidate"
            );
        } catch (DataAccessException ex) {
            log.warn(
                    "Could not publish voucher snapshot invalidation",
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
}
