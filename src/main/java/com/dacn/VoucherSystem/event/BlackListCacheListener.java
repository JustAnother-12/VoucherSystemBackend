package com.dacn.VoucherSystem.event;

import com.dacn.VoucherSystem.cache.BlackListStatusCache;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class BlackListCacheListener {
    private final BlackListStatusCache cache;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onChanged(BlackListChangedEvent event) {
        cache.evict(
                event.blacklistType(),
                event.targetValue()
        );
    }
}
