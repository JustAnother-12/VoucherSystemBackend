package com.dacn.VoucherSystem.controller;

import com.dacn.VoucherSystem.dto.request.FraudLogCreateRequest;
import com.dacn.VoucherSystem.dto.response.ApiResponse;
import com.dacn.VoucherSystem.dto.response.FraudLogResponse;
import com.dacn.VoucherSystem.service.interfaces.FraudLogService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/fraud-logs")
@Tag(name = "Fraud & Security", description = "Operations pertaining to Fraud logs and Black lists")
public class FraudLogController {

    private final FraudLogService service;

    @GetMapping
    public ResponseEntity<ApiResponse<List<FraudLogResponse>>> getAllFraudLogs() {
        List<FraudLogResponse> fraudLogs = service.getAllFraudLog();

        ApiResponse<List<FraudLogResponse>> response = ApiResponse.<List<FraudLogResponse>>builder()
                .status(HttpStatus.OK.value())
                .message("Get fraud logs successfully!")
                .data(fraudLogs)
                .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<FraudLogResponse>> getFraudLogById(@PathVariable Long id) {
        FraudLogResponse fraudLog = service.getFraudLogById(id);

        ApiResponse<FraudLogResponse> response = ApiResponse.<FraudLogResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Get fraud log successfully!")
                .data(fraudLog)
                .build();

        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<ApiResponse<FraudLogResponse>> create(
            @Valid @RequestBody FraudLogCreateRequest request
    ) {
        FraudLogResponse result = service.createManualFraudLog(
                request.voucherId(),
                request.customerId(),
                request.ipAddress(),
                request.deviceFingerprint(),
                request.violationRule()
        );

        ApiResponse<FraudLogResponse> response = ApiResponse.<FraudLogResponse>builder()
                        .status(HttpStatus.CREATED.value())
                        .message("Fraud log created successfully")
                        .data(result)
                        .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
