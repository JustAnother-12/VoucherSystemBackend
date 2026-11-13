package com.dacn.VoucherSystem.realtime;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

@Configuration
public class DashboardRealtimeConfig {

    @Bean
    RedisMessageListenerContainer dashboardEventContainer(
            RedisConnectionFactory connectionFactory,
            DashboardEventHub hub
    ) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(hub, new ChannelTopic(DashboardEventHub.CHANNEL));
        return container;
    }
}