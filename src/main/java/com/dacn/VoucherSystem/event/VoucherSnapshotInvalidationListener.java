package com.dacn.VoucherSystem.event;

import com.dacn.VoucherSystem.cache.VoucherSnapshotCache;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class VoucherSnapshotInvalidationListener {
    private final VoucherSnapshotCache snapshotCache;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onVoucherSnapshotsChanged(
            VoucherSnapshotsChangedEvent event
    ) {
        snapshotCache.invalidateAllSnapshots();
    }
}
