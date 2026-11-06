package com.dacn.VoucherSystem.controller;

import com.dacn.VoucherSystem.dto.request.CustomerVoucherCreateRequest;
import com.dacn.VoucherSystem.dto.request.CustomerVoucherUpdateRequest;
import com.dacn.VoucherSystem.dto.response.ApiResponse;
import com.dacn.VoucherSystem.dto.response.CustomerVoucherResponse;
import com.dacn.VoucherSystem.enums.CustomerVoucherStatus;
import com.dacn.VoucherSystem.service.interfaces.CustomerVoucherService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/customer-vouchers")
@Tag(name = "Customer Vouchers", description = "Operations pertaining to customer voucher wallets")
public class CustomerVoucherController {
    private final CustomerVoucherService customerVoucherService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<CustomerVoucherResponse>>> getAllCustomerVouchers(
            @RequestParam(required = false) String customerId,
            @RequestParam(required = false) CustomerVoucherStatus status
    ) {
        List<CustomerVoucherResponse> wallet = customerVoucherService.getAllCustomerVouchers(customerId, status);

        ApiResponse<List<CustomerVoucherResponse>> response = ApiResponse.<List<CustomerVoucherResponse>>builder()
                .status(HttpStatus.OK.value())
                .message("Get customer vouchers successfully!")
                .data(wallet)
                .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CustomerVoucherResponse>> getCustomerVoucherById(@PathVariable Long id) {
        CustomerVoucherResponse customerVoucher = customerVoucherService.getCustomerVoucherById(id);

        ApiResponse<CustomerVoucherResponse> response = ApiResponse.<CustomerVoucherResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Get customer voucher successfully!")
                .data(customerVoucher)
                .build();

        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CustomerVoucherResponse>> createCustomerVoucher(
            @Valid @RequestBody CustomerVoucherCreateRequest request
    ) {
        CustomerVoucherResponse customerVoucher = customerVoucherService.createCustomerVoucher(
                request.customerId(),
                request.voucherId()
        );

        ApiResponse<CustomerVoucherResponse> response = ApiResponse.<CustomerVoucherResponse>builder()
                .status(HttpStatus.CREATED.value())
                .message("Add voucher to customer wallet successfully!")
                .data(customerVoucher)
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<CustomerVoucherResponse>> updateCustomerVoucherStatus(
            @PathVariable Long id,
            @Valid @RequestBody CustomerVoucherUpdateRequest request
    ) {
        CustomerVoucherResponse customerVoucher = customerVoucherService.updateCustomerVoucherStatus(
                id,
                request.status()
        );

        ApiResponse<CustomerVoucherResponse> response = ApiResponse.<CustomerVoucherResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Update customer voucher status successfully!")
                .data(customerVoucher)
                .build();

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCustomerVoucher(@PathVariable Long id) {
        customerVoucherService.deleteCustomerVoucher(id);

        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .status(HttpStatus.OK.value())
                .message("Delete customer voucher successfully!")
                .build();

        return ResponseEntity.ok(response);
    }
}