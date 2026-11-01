package com.dacn.VoucherSystem.service.interfaces;

import com.dacn.VoucherSystem.dto.request.FraudLogCreateRequest;
import com.dacn.VoucherSystem.dto.response.FraudLogResponse;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public interface FraudLogService {
    List<FraudLogResponse> getAllFraudLog();
    FraudLogResponse getFraudLogById(Long id);
    FraudLogResponse createManualFraudLog(
            Long voucherId,
            String customerId,
            String ipAddress,
            String deviceFingerprint,
            String violationRule
    );

    void recordAutomatedFraud(
            Long voucherId,
            String customerId,
            String ipAddress,
            String deviceFingerprint,
            String violationRule
    );
}
