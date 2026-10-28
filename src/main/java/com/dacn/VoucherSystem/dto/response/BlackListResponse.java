package com.dacn.VoucherSystem.dto.response;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@JsonPropertyOrder({
        "id",
        "type",
        "value",
        "reason",
        "isActive",
        "expiredAt",
        "createAt"
})
public class BlackListResponse {
    private Long id;
    private String type;
    private String value;
    private String reason;
    private Boolean isActive;
    private LocalDateTime expiredAt;
    private LocalDateTime createAt;
}
