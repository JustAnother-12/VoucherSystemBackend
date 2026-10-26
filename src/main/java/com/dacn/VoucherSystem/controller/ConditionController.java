package com.dacn.VoucherSystem.controller;

import com.dacn.VoucherSystem.dto.request.ConditionCreateRequest;
import com.dacn.VoucherSystem.dto.request.ConditionUpdateRequest;
import com.dacn.VoucherSystem.dto.response.ApiResponse;
import com.dacn.VoucherSystem.dto.response.ConditionResponse;
import com.dacn.VoucherSystem.service.ConditionService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/rule/condition")
@Tag(name = "Rule Engine", description = "Operations pertaining to rules, rewards, and conditions")
public class ConditionController {
    private final ConditionService service;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ConditionResponse>>> getAllConfigs() {
        List<ConditionResponse> conditions = service.getAllCondition();

        ApiResponse<List<ConditionResponse>> response = ApiResponse.<List<ConditionResponse>>builder()
                .status(HttpStatus.OK.value())
                .message("Get conditions successfully!")
                .data(conditions)
                .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{condition_id}")
    public ResponseEntity<ApiResponse<ConditionResponse>> getConfigById(@PathVariable Long condition_id) {
        ConditionResponse condition = service.getConditionById(condition_id);

        ApiResponse<ConditionResponse> response = ApiResponse.<ConditionResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Get condition successfully!")
                .data(condition)
                .build();

        return ResponseEntity.ok(response);
    }

    @PostMapping()
    public ResponseEntity<ApiResponse<ConditionResponse>> createConfig(
            @Valid @RequestBody ConditionCreateRequest request
    ){
        ConditionResponse condition = service.createCondition(
                request.getName(),
                request.getType(),
                request.getConfig()
        );

        ApiResponse<ConditionResponse> response = ApiResponse.<ConditionResponse>builder()
                .status(HttpStatus.CREATED.value())
                .message("Create condition successfully!")
                .data(condition)
                .build();

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{condition_id}")
    public ResponseEntity<ApiResponse<ConditionResponse>> updateConfig(
            @PathVariable long condition_id,
            @Valid @RequestBody ConditionUpdateRequest request
    ){
        ConditionResponse condition = service.updateCondition(
                condition_id,
                request.getName(),
                request.getType(),
                request.getConfig()
        );

        ApiResponse<ConditionResponse> response = ApiResponse.<ConditionResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Update condition successfully!")
                .data(condition)
                .build();

        return ResponseEntity.ok(response);
    }
}
