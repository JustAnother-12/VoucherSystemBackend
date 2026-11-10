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
        if (event.active()) {
            cache.upsert(
                    event.blacklistType(),
                    event.targetValue(),
                    event.expiredAt()
            );

        } else {
            cache.remove(
                    event.blacklistType(),
                    event.targetValue()
            );
        }
    }
}
