package com.dacn.VoucherSystem.controller;

import com.dacn.VoucherSystem.dto.request.VoucherRedeemRequest;
import com.dacn.VoucherSystem.dto.response.ApiResponse;
import com.dacn.VoucherSystem.dto.response.RedemptionLogResponse;
import com.dacn.VoucherSystem.dto.response.VoucherRedeemResponse;
import com.dacn.VoucherSystem.service.RedemptionService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/redemptions")
@Tag(name = "Redemptions", description = "Operations pertaining to Redeem and Redemption logs")
public class RedemptionController {
    private final RedemptionService redemptionService;

    @GetMapping("/logs")
    public ResponseEntity<ApiResponse<List<RedemptionLogResponse>>> getAllConfigs() {
        List<RedemptionLogResponse> rewards = redemptionService.getAllRedemptionLog();

        ApiResponse<List<RedemptionLogResponse>> response = ApiResponse.<List<RedemptionLogResponse>>builder()
                .status(HttpStatus.OK.value())
                .message("Get redemption logs successfully!")
                .data(rewards)
                .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/logs/{id}")
    public ResponseEntity<ApiResponse<RedemptionLogResponse>> getConfigById(@PathVariable Long id) {
        RedemptionLogResponse reward = redemptionService.getRedemptionLogById(id);

        ApiResponse<RedemptionLogResponse> response = ApiResponse.<RedemptionLogResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Get redemption log successfully!")
                .data(reward)
                .build();

        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<ApiResponse<VoucherRedeemResponse>> applyVoucher(
            @Valid @RequestBody VoucherRedeemRequest request
    ) {
        VoucherRedeemResponse result =
                redemptionService.redeemVoucher(
                        request.customerId(),
                        request.voucherCode(),
                        request.cartSubtotal(),
                        request.customerSegments(),
                        request.items(),
                        request.ipAddress(),
                        request.deviceFingerprint()
                );

        ApiResponse<VoucherRedeemResponse> response =
                ApiResponse.<VoucherRedeemResponse>builder()
                        .status(HttpStatus.OK.value())
                        .message("Voucher applied successfully")
                        .data(result)
                        .build();

        return ResponseEntity.ok(response);
    }
}
