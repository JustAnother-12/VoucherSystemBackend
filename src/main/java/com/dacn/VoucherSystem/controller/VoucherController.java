package com.dacn.VoucherSystem.controller;

import com.dacn.VoucherSystem.dto.response.ApiResponse;
import com.dacn.VoucherSystem.dto.response.VoucherResponse;
import com.dacn.VoucherSystem.service.interfaces.VoucherService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/vouchers")
@Tag(name = "Vouchers", description = "Operations pertaining to Vouchers and customer voucher wallets")
public class VoucherController {
    private final VoucherService voucherService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<VoucherResponse>>> getAllVouchers(
            @RequestParam(required = false) Long batchId,
            @RequestParam(required = false) Long campaignId
    ) {
        List<VoucherResponse> vouchers = voucherService.getAllVouchers(batchId, campaignId);

        ApiResponse<List<VoucherResponse>> response = ApiResponse.<List<VoucherResponse>>builder()
                .status(HttpStatus.OK.value())
                .message("Get vouchers successfully!")
                .data(vouchers)
                .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<VoucherResponse>> getVoucherById(@PathVariable Long id) {
        VoucherResponse voucher = voucherService.getVoucherById(id);

        ApiResponse<VoucherResponse> response = ApiResponse.<VoucherResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Get voucher successfully!")
                .data(voucher)
                .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/code/{code}")
    public ResponseEntity<ApiResponse<VoucherResponse>> getVoucherByCode(@PathVariable String code) {
        VoucherResponse voucher = voucherService.getVoucherByCode(code);

        ApiResponse<VoucherResponse> response = ApiResponse.<VoucherResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Get voucher successfully!")
                .data(voucher)
                .build();

        return ResponseEntity.ok(response);
    }
}