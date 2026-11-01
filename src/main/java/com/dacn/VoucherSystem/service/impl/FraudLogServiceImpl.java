package com.dacn.VoucherSystem.service.impl;

import com.dacn.VoucherSystem.dto.response.FraudLogResponse;
import com.dacn.VoucherSystem.entity.FraudLog;
import com.dacn.VoucherSystem.entity.Voucher;
import com.dacn.VoucherSystem.exception.ResourceNotFoundException;
import com.dacn.VoucherSystem.mapper.FraudLogMapper;
import com.dacn.VoucherSystem.repository.FraudLogRepository;
import com.dacn.VoucherSystem.repository.VoucherRepository;
import com.dacn.VoucherSystem.service.interfaces.FraudLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FraudLogServiceImpl implements FraudLogService {
    private final FraudLogRepository fraudLogRepository;
    private final VoucherRepository voucherRepository;
    private final FraudLogMapper fraudLogMapper;

    @Override
    @Transactional(readOnly = true)
    public List<FraudLogResponse> getAllFraudLog() {
        List<FraudLog> configs = fraudLogRepository.findAll();

        return configs.stream()
                .map(fraudLogMapper::toFraudLogResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public FraudLogResponse getFraudLogById(Long id) {
        FraudLog config = fraudLogRepository
                .findById(id)
                .orElseThrow(
                        () -> new ResourceNotFoundException("No fraud log with id " + id + " found in the system")
                );

        return fraudLogMapper.toFraudLogResponse(config);
    }

    @Override
    @Transactional
    public FraudLogResponse createManualFraudLog(
            Long voucherId,
            String customerId,
            String ipAddress,
            String deviceFingerprint,
            String violationRule
    ) {
        FraudLog fraudLog = createFraudFromVoucher(
                voucherId,
                customerId,
                ipAddress,
                deviceFingerprint,
                violationRule
        );
        fraudLogRepository.saveAndFlush(fraudLog);
        return fraudLogMapper.toFraudLogResponse(fraudLog);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordAutomatedFraud(
            Long voucherId,
            String customerId,
            String ipAddress,
            String deviceFingerprint,
            String violationRule
    ) {
        FraudLog fraudLog = createFraudFromVoucher(
                voucherId,
                customerId,
                ipAddress,
                deviceFingerprint,
                violationRule
        );

        fraudLogRepository.save(fraudLog);
    }

    private FraudLog createFraudFromVoucher(
            Long voucherId,
            String customerId,
            String ipAddress,
            String deviceFingerprint,
            String violationRule
    ){
        Voucher voucher = voucherId == null
                ? null
                : voucherRepository.findById(voucherId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Voucher not found: " + voucherId
                ));

        return FraudLog.builder()
                .voucher(voucher)
                .customerId(customerId)
                .ipAddress(ipAddress)
                .deviceFingerprint(deviceFingerprint)
                .violationRule(violationRule)
                .build();
    }
}
