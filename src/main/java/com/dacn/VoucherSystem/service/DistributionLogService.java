package com.dacn.VoucherSystem.service;

import com.dacn.VoucherSystem.dto.response.DistributionLogResponse;
import java.util.List;

public interface DistributionLogService {
    List<DistributionLogResponse> getAllRateLimitConfig();
    DistributionLogResponse getRateLimitConfigById(Long id);
}
