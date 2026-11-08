package com.dacn.VoucherSystem.controller;

import com.dacn.VoucherSystem.dto.request.DistributionLogCreateRequest;
import com.dacn.VoucherSystem.dto.response.ApiResponse;
import com.dacn.VoucherSystem.dto.response.DistributionLogResponse;
import com.dacn.VoucherSystem.enums.DistributionStatus;
import com.dacn.VoucherSystem.service.interfaces.DistributionLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/distributions")
@Tag(name = "Distribution", description = "Operations pertaining to Distribution logs")
public class DistributionLogController {
    private final DistributionLogService service;

    @GetMapping
    @Operation(summary = "Lấy danh sách nhật ký phân phối")
    public ResponseEntity<ApiResponse<List<DistributionLogResponse>>> getAllLogs(
            @RequestParam(name = "voucher_id", required = false) Long voucherId,
            @RequestParam(name = "campaign_id", required = false) Long campaignId,
            @RequestParam(name = "status", required = false) DistributionStatus status
    ) {
        List<DistributionLogResponse> logs = service.getAllDistributionLog(voucherId, campaignId, status);

        ApiResponse<List<DistributionLogResponse>> response = ApiResponse.<List<DistributionLogResponse>>builder()
                .status(HttpStatus.OK.value())
                .message("Get distribution logs successfully!")
                .data(logs)
                .build();

        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<ApiResponse<DistributionLogResponse>> createDistribution(
            @Valid @RequestBody DistributionLogCreateRequest request
    ) {
        DistributionLogResponse log = service.createDistribution(
                request.batchId(),
                request.campaignId(),
                request.channel(),
                request.targetAddress()
        );

        ApiResponse<DistributionLogResponse> response = ApiResponse.<DistributionLogResponse>builder()
                .status(HttpStatus.CREATED.value())
                .message("Create distribution successfully!")
                .data(log)
                .build();

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping("/logs/{id}")
    public ResponseEntity<ApiResponse<DistributionLogResponse>> getLogById(
            @PathVariable Long id
    ) {
        DistributionLogResponse log = service.getDistributionLogById(id);

        ApiResponse<DistributionLogResponse> response = ApiResponse.<DistributionLogResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Get distribution log successfully!")
                .data(log)
                .build();

        return ResponseEntity.ok(response);
    }
}