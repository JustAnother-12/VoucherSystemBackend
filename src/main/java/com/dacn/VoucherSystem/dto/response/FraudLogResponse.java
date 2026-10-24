package com.dacn.VoucherSystem.dto.response;

import com.dacn.VoucherSystem.entity.Voucher;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FraudLogResponse {
    private Long id;
    private Long voucherId;
    private String customerId;
    private String ipAddress;
    private String deviceFingerprint;
    private String violationRule;
}
