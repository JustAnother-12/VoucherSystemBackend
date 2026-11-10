package com.dacn.VoucherSystem.cache;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class RedisLocalCacheInvalidationSubscriber implements MessageListener {

    private final RateLimitPolicyCache rateLimitPolicyCache;
    private final VoucherSnapshotCache voucherSnapshotCache;

    @Override
    public void onMessage(
            Message message,
            byte[] pattern
    ) {

        String channel = new String(
                message.getChannel(),
                StandardCharsets.UTF_8
        );

        switch (channel) {

            case RateLimitPolicyCache.INVALIDATION_CHANNEL ->
                    rateLimitPolicyCache.invalidateLocal();

            case VoucherSnapshotCache.INVALIDATION_CHANNEL ->
                    voucherSnapshotCache.invalidateLocal();

            default -> {
                // Ignore unknown channels.
            }
        }
    }
}
