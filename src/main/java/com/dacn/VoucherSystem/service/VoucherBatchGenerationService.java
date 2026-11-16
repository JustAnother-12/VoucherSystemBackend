package com.dacn.VoucherSystem.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class VoucherBatchGenerationService {
    private final VoucherBatchGenerationWorker generationWorker;

    @Async("voucherGenerationExecutor")
    public CompletableFuture<Void> generate(Long batchId) {
        try {
            generationWorker.generate(batchId);
            return CompletableFuture.completedFuture(null);
        } catch (RuntimeException exception) {
            log.error("Failed to generate vouchers for batch {}", batchId, exception);
            throw exception;
        }
    }
}
