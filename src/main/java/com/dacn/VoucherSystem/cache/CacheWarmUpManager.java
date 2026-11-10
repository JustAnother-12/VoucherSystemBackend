package com.dacn.VoucherSystem.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class CacheWarmUpManager implements ApplicationRunner {
    private final List<CacheWarmUpTask> tasks;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        for (CacheWarmUpTask task : tasks) {
            try {
                task.warmUp();
            } catch (RuntimeException ex) {
                log.error(
                        "Cache warm-up task '{}' failed. Aborting application startup.",
                        task.getName(),
                        ex
                );
                throw new IllegalStateException(
                        "Failed to warm up cache: " + task.getName(),
                        ex
                );
            }
        }
    }
}
