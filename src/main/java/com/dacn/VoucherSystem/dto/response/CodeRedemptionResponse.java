package com.dacn.VoucherSystem.dto.response;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Builder;

@Builder
@JsonPropertyOrder({
        "code",
        "issuedCount",
        "redeemedCount",
        "redemptionRate",
        "distributedCount",
        "distributedRedemptionRate"
})
public record CodeRedemptionResponse(
        String code,
        long issuedCount,
        long redeemedCount,
        double redemptionRate,
        long distributedCount,
        double distributedRedemptionRate
) {
}
