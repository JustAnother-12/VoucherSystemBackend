package com.dacn.VoucherSystem.service.interfaces;

import com.dacn.VoucherSystem.dto.response.FraudLogResponse;
import java.util.List;

public interface FraudLogService {
    List<FraudLogResponse> getAllFraudLog();
    FraudLogResponse getFraudLogById(Long id);
}
