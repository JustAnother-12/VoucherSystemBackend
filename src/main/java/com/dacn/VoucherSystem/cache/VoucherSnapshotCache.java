package com.dacn.VoucherSystem.cache;

import com.dacn.VoucherSystem.entity.*;
import com.dacn.VoucherSystem.exception.ResourceNotFoundException;
import com.dacn.VoucherSystem.repository.VoucherRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class VoucherSnapshotCache {

    private static final Duration TTL = Duration.ofSeconds(10);
    private static final String VERSION_KEY = "voucher:snapshot:version";

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final VoucherRepository voucherRepository;

    private String getCacheVersion() {
        try {
            redis.opsForValue().setIfAbsent(VERSION_KEY, "1");
            return redis.opsForValue().get(VERSION_KEY);

        } catch (RedisConnectionFailureException ex) {
            log.warn("Redis unavailable while reading voucher cache version");
            return null;
        }
    }

    public void invalidateAllSnapshots() {
        try {
            redis.opsForValue().increment(VERSION_KEY);

        } catch (RedisConnectionFailureException ex) {
            log.warn("Could not invalidate voucher snapshots because Redis is unavailable");
        }
    }

    public VoucherRuleSnapshot getByCode(String rawCode) {
        String code = rawCode.trim().toUpperCase(Locale.ROOT);

        String version = getCacheVersion();

        String key = version == null
                ? null
                : "voucher:voucherCode:" + code + ":v" + version;

        String cached = null;

        if (key != null) {
            try {
                cached = redis.opsForValue().get(key);
            } catch (RedisConnectionFailureException ex) {
                log.warn("Redis unavailable while reading voucher snapshot");
            }
        }

        if (cached != null) {
            try {
                return objectMapper.readValue(cached, VoucherRuleSnapshot.class);
            } catch (Exception ex) {
                try {
                    redis.delete(key);
                } catch (RedisConnectionFailureException ignored) {
                    log.warn("Fails to delete redis key:{}", key);
                }
            }
        }

        Voucher voucher = voucherRepository.findVoucherByCode(code)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Voucher voucherCode not found")
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

        VoucherRuleSnapshot snapshot = new VoucherRuleSnapshot(
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
                condition.getConditionJson().toString(),
                reward.getRewardType(),
                reward.getStatus(),
                reward.getRewardJson().toString()
        );

        if (key != null) {
            try {
                redis.opsForValue().set(
                        key,
                        objectMapper.writeValueAsString(snapshot),
                        TTL
                );
            } catch (Exception ex) {
                log.warn("Could not cache voucher snapshot", ex);
            }
        }

        return snapshot;
    }
}
