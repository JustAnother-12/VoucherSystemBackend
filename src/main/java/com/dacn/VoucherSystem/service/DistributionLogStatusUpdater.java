package com.dacn.VoucherSystem.service;

import com.dacn.VoucherSystem.entity.DistributionLog;
import com.dacn.VoucherSystem.enums.DistributionStatus;
import com.dacn.VoucherSystem.exception.ResourceNotFoundException;
import com.dacn.VoucherSystem.repository.DistributionLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class DistributionLogStatusUpdater {
    private final DistributionLogRepository repository;

    @Transactional
    public void updateStatus(Long logId, DistributionStatus status) {
        DistributionLog distributionLog = repository.findById(logId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No distribution log with id " + logId + " found in the system"));
        distributionLog.setStatus(status);
        distributionLog.setSentAt(LocalDateTime.now());
    }
}
