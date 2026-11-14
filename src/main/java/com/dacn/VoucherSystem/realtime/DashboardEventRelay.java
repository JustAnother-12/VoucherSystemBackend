package com.dacn.VoucherSystem.realtime;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class DashboardEventRelay {
    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void relay(DashboardEvent event) {
        try {
            redis.convertAndSend(DashboardEventHub.CHANNEL, objectMapper.writeValueAsString(event));
        } catch (RuntimeException ex) {
            // Lỗi phát sự kiện không được làm hỏng giao dịch áp mã
            log.warn("Không publish được sự kiện dashboard", ex);
        }
    }
}