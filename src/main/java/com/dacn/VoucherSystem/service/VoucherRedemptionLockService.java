package com.dacn.VoucherSystem.service;

import com.dacn.VoucherSystem.exception.RedemptionConflictException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.redisson.client.RedisException;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Slf4j
@Service
@RequiredArgsConstructor
public class VoucherRedemptionLockService {
    private final RedissonClient redissonClient;

    public <T> T execute(
            Long voucherId,
            String customerId,
            Supplier<T> action
    ) {
        String lockKey = "redemption:voucher:"
                + voucherId
                + ":customer:"
                + customerId;

        RLock lock = redissonClient.getLock(lockKey);

        boolean acquired;

        try {
            acquired = lock.tryLock(2, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();

            throw new RedemptionConflictException(
                    "Interrupted while waiting for redemption lock"
            );

        } catch (RedisException ex) {
            log.error("Redis lock service is unavailable", ex);
            throw new IllegalStateException(
                    "Redemption service is temporarily unavailable",
                    ex
            );
        }

        if (!acquired) {
            throw new RedemptionConflictException(
                    "Voucher is currently being processed"
            );
        }

        try {
            return action.get();
        } finally {
            try {
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            } catch (RedisException ex) {
                log.warn("Could not release redemption lock: {}", lockKey, ex);
            }
        }
    }
}
