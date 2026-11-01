package com.dacn.VoucherSystem.event;

import com.dacn.VoucherSystem.cache.RateLimitPolicyCache;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class RateLimitConfigCacheListener {

    private final RateLimitPolicyCache policyCache;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onChanged(RateLimitConfigsChangedEvent event) {
        policyCache.invalidate();
    }
}
