package com.dacn.VoucherSystem.dto.response;

import com.dacn.VoucherSystem.enums.DistributionStatus;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@JsonPropertyOrder({
        "id",
        "voucherId",
        "channel",
        "targetAddress",
        "status",
        "sentAt"
})
public class DistributionLogResponse {
    private Long id;
    private Long voucherId;
    private String channel;
    private String targetAddress;
    private DistributionStatus status;
    private LocalDateTime sentAt;
}
