package com.dacn.VoucherSystem.service;

import com.dacn.VoucherSystem.dto.response.FraudLogResponse;
import java.util.List;

public interface FraudLogService {
    List<FraudLogResponse> getAllRateLimitConfig();
    FraudLogResponse getRateLimitConfigById(Long id);
}
