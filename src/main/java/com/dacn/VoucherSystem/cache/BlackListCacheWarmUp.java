package com.dacn.VoucherSystem.cache;

import com.dacn.VoucherSystem.entity.BlackList;
import com.dacn.VoucherSystem.repository.BlackListRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@Order(10)
public class BlackListCacheWarmUp implements CacheWarmUpTask{
    private final BlackListRepository repository;
    private final BlackListStatusCache cache;
    private final Clock clock;

    @Override
    public String getName() {
        return "BlackListCache";
    }

    @Override
    public void warmUp() {
        List<BlackList> activeRecords = repository.findCurrentlyActive(LocalDateTime.now(clock));

        cache.warmUp(activeRecords);

        log.info(
                "Blacklist Redis index initialized with {} records",
                activeRecords.size()
        );
    }
}
