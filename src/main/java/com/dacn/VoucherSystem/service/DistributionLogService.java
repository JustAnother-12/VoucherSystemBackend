package com.dacn.VoucherSystem.service;

import com.dacn.VoucherSystem.dto.response.DistributionLogResponse;
import java.util.List;

public interface DistributionLogService {
    List<DistributionLogResponse> getAllDistributionLog();
    DistributionLogResponse getDistributionLogById(Long id);
}
