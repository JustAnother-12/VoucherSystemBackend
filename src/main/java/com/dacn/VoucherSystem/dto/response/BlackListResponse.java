package com.dacn.VoucherSystem.dto.response;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@JsonPropertyOrder({
        "id",
        "name",
        "endpointPattern",
        "scope",
        "maxRequest",
        "timeWindowSeconds",
        "actionOnExceed",
        "priority",
        "isActive",
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
