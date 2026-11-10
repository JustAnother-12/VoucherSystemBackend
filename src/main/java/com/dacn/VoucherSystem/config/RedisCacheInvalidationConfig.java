package com.dacn.VoucherSystem.config;

import com.dacn.VoucherSystem.cache.RateLimitPolicyCache;
import com.dacn.VoucherSystem.cache.RedisLocalCacheInvalidationSubscriber;
import com.dacn.VoucherSystem.cache.VoucherSnapshotCache;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

@Configuration
@RequiredArgsConstructor
public class RedisCacheInvalidationConfig {

    @Bean
    public RedisMessageListenerContainer cacheInvalidationContainer(
            RedisConnectionFactory connectionFactory,
            RedisLocalCacheInvalidationSubscriber subscriber
    ) {

        RedisMessageListenerContainer container =
                new RedisMessageListenerContainer();

        container.setConnectionFactory(connectionFactory);

        container.addMessageListener(
                subscriber,
                new ChannelTopic(
                        RateLimitPolicyCache.INVALIDATION_CHANNEL
                )
        );

        container.addMessageListener(
                subscriber,
                new ChannelTopic(
                        VoucherSnapshotCache.INVALIDATION_CHANNEL
                )
        );

        return container;
    }
}
