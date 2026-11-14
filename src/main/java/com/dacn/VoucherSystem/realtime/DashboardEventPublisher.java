package com.dacn.VoucherSystem.realtime;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DashboardEventPublisher {
    private final ApplicationEventPublisher applicationEventPublisher;

    public void publish(DashboardEvent event) {
        applicationEventPublisher.publishEvent(event);
    }
}