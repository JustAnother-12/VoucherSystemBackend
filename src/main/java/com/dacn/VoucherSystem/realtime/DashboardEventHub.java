package com.dacn.VoucherSystem.realtime;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

// Giữ các kết nối SSE đang mở trên instance này. Sự kiện đến từ Redis nên hoạt động với nhiều instance.
@Slf4j
@Component
public class DashboardEventHub implements MessageListener {
    public static final String CHANNEL = "dashboard.events";
    // Nhỏ hơn thời hạn access token (2 giờ), để FE kết nối lại và BFF làm mới token
    private static final long TIMEOUT_MS = 30L * 60 * 1000;

    private final Set<SseEmitter> emitters = ConcurrentHashMap.newKeySet();
    private final ScheduledExecutorService heartbeat =
            Executors.newSingleThreadScheduledExecutor();

    @PostConstruct
    void startHeartbeat() {
        // Gửi comment định kỳ để proxy không cắt kết nối rỗi
        heartbeat.scheduleAtFixedRate(
                () -> emitters.forEach(e -> {
                    try {
                        e.send(SseEmitter.event().comment("keepalive"));
                    } catch (IOException | IllegalStateException ex) {
                        emitters.remove(e);
                    }
                }),
                25, 25, TimeUnit.SECONDS
        );
    }

    @PreDestroy
    void stopHeartbeat() {
        heartbeat.shutdownNow();
    }

    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(TIMEOUT_MS);
        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(ex -> emitters.remove(emitter));
        send(emitter, "connected", "ok");
        return emitter;
    }

    // Nhận từ Redis rồi phát cho mọi kết nối đang mở trên instance này
    @Override
    public void onMessage(Message message, byte[] pattern) {
        String json = new String(message.getBody(), StandardCharsets.UTF_8);
        emitters.forEach(e -> send(e, "dashboard", json));
    }

    private void send(SseEmitter emitter, String name, String data) {
        try {
            emitter.send(SseEmitter.event().name(name).data(data));
        } catch (IOException | IllegalStateException ex) {
            emitters.remove(emitter);
        }
    }
}