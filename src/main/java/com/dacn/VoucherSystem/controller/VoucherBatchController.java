package com.dacn.VoucherSystem.controller;

import com.dacn.VoucherSystem.dto.request.VoucherBatchCreateRequest;
import com.dacn.VoucherSystem.dto.request.VoucherBatchUpdateRequest;
import com.dacn.VoucherSystem.dto.response.ApiResponse;
import com.dacn.VoucherSystem.dto.response.VoucherBatchResponse;
import com.dacn.VoucherSystem.service.interfaces.VoucherBatchService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/voucher-batches")
@Tag(name = "Voucher Batches", description = "Operations pertaining to Voucher batches and Batches rules")
public class VoucherBatchController {
    private final VoucherBatchService voucherBatchService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<VoucherBatchResponse>>> getAllVoucherBatches(){
        List<VoucherBatchResponse> voucherBatches = voucherBatchService.getAllVoucherBatches();

        ApiResponse<List<VoucherBatchResponse>> response = ApiResponse.<List<VoucherBatchResponse>>builder()
                .status(HttpStatus.OK.value())
                .message("Get Voucher Batches successfully!")
                .data(voucherBatches)
                .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<VoucherBatchResponse>> getVoucherBatchByID(@PathVariable Long id){
        VoucherBatchResponse voucherBatch = voucherBatchService.getVoucherBatchByID(id);

        ApiResponse<VoucherBatchResponse> response = ApiResponse.<VoucherBatchResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Get Voucher Batch successfully!")
                .data(voucherBatch)
                .build();

        return ResponseEntity.ok(response);
    }


    @PostMapping()
    public ResponseEntity<ApiResponse<VoucherBatchResponse>> createVoucherBatch(

            @Valid @RequestBody VoucherBatchCreateRequest request
    ){
        VoucherBatchResponse voucherBatch = voucherBatchService.createVoucherBatch(
                request.campaignId(),
                request.ruleId(),
                request.batchType(),
                request.codeConfig(),
                request.durationUnit(),
                request.durationValue(),
                request.distributionChannels()
        );

        ApiResponse<VoucherBatchResponse> response = ApiResponse.<VoucherBatchResponse>builder()
                .status(HttpStatus.CREATED.value())
                .message("Create Voucher Batch successfully!")
                .data(voucherBatch)
                .build();

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<VoucherBatchResponse>> updateVoucherBatch(
            @PathVariable long id,
            @Valid @RequestBody VoucherBatchUpdateRequest request
    ){
        VoucherBatchResponse voucherBatch = voucherBatchService.updateVoucherBatch(
                id,
                request.ruleId(),
                request.batchType(),
                request.codeConfig(),
                request.durationUnit(),
                request.durationValue(),
                request.distributionChannels(),
                request.status()
        );

        ApiResponse<VoucherBatchResponse> response = ApiResponse.<VoucherBatchResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Update Voucher Batch successfully!")
                .data(voucherBatch)
                .build();

        return ResponseEntity.ok(response);
    }

}
