package com.dacn.VoucherSystem.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class BlackListResponse {
    private Long id;
    private String type;
    private String value;
    private String reason;
    private Boolean isActive;
    private LocalDateTime expiredAt;
}
