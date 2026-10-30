package com.dacn.VoucherSystem.service;

import com.dacn.VoucherSystem.dto.request.VoucherRedeemRequest;
import com.dacn.VoucherSystem.dto.response.RedemptionLogResponse;
import com.dacn.VoucherSystem.dto.response.VoucherRedeemResponse;
import jakarta.validation.Valid;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

public interface RedemptionService {
    List<RedemptionLogResponse> getAllRedemptionLog();
    RedemptionLogResponse getRedemptionLogById(Long id);
    VoucherRedeemResponse redeemVoucher(
            String customerId,
            String code,
            BigDecimal cartSubtotal,
            Set<String> customerSegments,
            List<VoucherRedeemRequest.CartItemRequest> items,
            String ipAddress,
            String deviceFingerprint
    );
}
