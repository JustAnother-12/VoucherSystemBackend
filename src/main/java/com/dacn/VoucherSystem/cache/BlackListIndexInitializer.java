package com.dacn.VoucherSystem.cache;

import com.dacn.VoucherSystem.entity.BlackList;
import com.dacn.VoucherSystem.repository.BlackListRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class BlackListIndexInitializer implements ApplicationRunner {
    private final BlackListRepository repository;
    private final BlackListStatusCache cache;
    private final Clock clock;

    @Override
    public void run(ApplicationArguments args) {

        List<BlackList> activeRecords =
                repository.findCurrentlyActive(
                        LocalDateTime.now(clock)
                );

        cache.warmUp(activeRecords);

        log.info(
                "Blacklist Redis index initialized with {} records",
                activeRecords.size()
        );
    }
}
