package com.dacn.VoucherSystem.cache;

import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
@Order(20)
public class RateLimitCacheWarmUp implements CacheWarmUpTask{
    private final RateLimitPolicyCache cache;

    @Override
    public String getName() {
        return "RateLimitCache";
    }

    @Override
    public void warmUp() {
        cache.warmUp();
    }
}
