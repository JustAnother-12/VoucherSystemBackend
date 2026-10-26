package com.dacn.VoucherSystem.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@JsonPropertyOrder({
        "id",
        "voucherId",
        "customerId",
        "ipAddress",
        "deviceFingerprint",
        "violationRule",
        "create_at"
})
public class FraudLogResponse {
    private Long id;
    private Long voucherId;
    private String customerId;
    private String ipAddress;
    private String deviceFingerprint;
    private String violationRule;
    private LocalDateTime createAt;
}
