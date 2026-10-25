package com.dacn.VoucherSystem.dto.response;

import com.dacn.VoucherSystem.entity.Campaign;
import com.dacn.VoucherSystem.entity.Voucher;
import com.dacn.VoucherSystem.enums.DistributionStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class DistributionLogResponse {
    private Long id;
    private Long voucherId;
    private String channel;
    private String targetAddress;
    private DistributionStatus status;
    private LocalDateTime sentAt;
}
