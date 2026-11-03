package com.dacn.VoucherSystem.dto.response;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Builder;

import java.util.List;

@Builder
@JsonPropertyOrder({
        "batchId",
        "issuedCount",
        "redeemedCount",
        "redemptionRate",
        "codes"
})
public record BatchRedemptionResponse(
        String batchId,
        long issuedCount,
        long redeemedCount,
        double redemptionRate,
        List<CodeRedemptionResponse> codes
) {
}
