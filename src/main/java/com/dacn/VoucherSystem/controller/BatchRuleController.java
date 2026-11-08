package com.dacn.VoucherSystem.controller;

import com.dacn.VoucherSystem.dto.request.BatchRuleCreateRequest;
import com.dacn.VoucherSystem.dto.request.BatchRuleUpdateRequest;
import com.dacn.VoucherSystem.dto.response.ApiResponse;
import com.dacn.VoucherSystem.dto.response.BatchRuleResponse;
import com.dacn.VoucherSystem.service.interfaces.BatchRuleService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/batch-rules")
@RequiredArgsConstructor
@Tag(name = "Voucher Batches", description = "Operations pertaining to Voucher batches and Batches rules")
public class BatchRuleController {

    private final BatchRuleService batchRuleService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<BatchRuleResponse>>> getAllBatchRules(
            @RequestParam(name = "reward_id", required = false) Long rewardId,
            @RequestParam(name = "condition_id", required = false) Long conditionId
    ) {
        List<BatchRuleResponse> batchRules = batchRuleService.getAllBatchRules(rewardId, conditionId);

        ApiResponse<List<BatchRuleResponse>> response = ApiResponse.<List<BatchRuleResponse>>builder()
                .status(HttpStatus.OK.value())
                .message("Get Batch Rules successfully!")
                .data(batchRules)
                .build();

        return ResponseEntity.ok(response);

    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BatchRuleResponse>> getBatchRuleById(
            @PathVariable Long id
    ) {
        BatchRuleResponse batchRule = batchRuleService.getBatchRuleById(id);

        ApiResponse<BatchRuleResponse> response = ApiResponse.<BatchRuleResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Get Batch Rules successfully!")
                .data(batchRule)
                .build();

        return ResponseEntity.ok(response);

    }

    @PostMapping
    public ResponseEntity<ApiResponse<BatchRuleResponse>> createBatchRule(
            @Valid @RequestBody BatchRuleCreateRequest request
    ) {
        BatchRuleResponse batchRule = batchRuleService.createBatchRule(
                request.rewardId(),
                request.conditionId()
        );

        ApiResponse<BatchRuleResponse> response = ApiResponse.<BatchRuleResponse>builder()
                .status(HttpStatus.CREATED.value())
                .message("Create Batch Rule successfully!")
                .data(batchRule)
                .build();

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<BatchRuleResponse>> updateBatchRule(
            @PathVariable Long id,
            @RequestBody BatchRuleUpdateRequest request
    ) {
        BatchRuleResponse batchRule = batchRuleService.updateBatchRule(
                id,
                request.rewardId(),
                request.conditionId()
        );

        ApiResponse<BatchRuleResponse> response = ApiResponse.<BatchRuleResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Update Batch Rule successfully!")
                .data(batchRule)
                .build();

        return ResponseEntity.ok(response);
    }
}