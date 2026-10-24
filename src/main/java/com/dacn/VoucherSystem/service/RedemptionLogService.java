package com.dacn.VoucherSystem.service;

import com.dacn.VoucherSystem.dto.response.RedemptionLogResponse;
import java.util.List;

public interface RedemptionLogService {
    List<RedemptionLogResponse> getAllRateLimitConfig();
    RedemptionLogResponse getRateLimitConfigById(Long id);
}
