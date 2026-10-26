package com.dacn.VoucherSystem.controller;

import com.dacn.VoucherSystem.dto.request.RateLimitConfigCreateRequest;
import com.dacn.VoucherSystem.dto.request.RateLimitConfigUpdateRequest;
import com.dacn.VoucherSystem.dto.response.ApiResponse;
import com.dacn.VoucherSystem.dto.response.RateLimitConfigResponse;
import com.dacn.VoucherSystem.service.RateLimitConfigService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/rate_limit_config")
@Tag(name = "Rate Limit", description = "Operations pertaining Rate Limiting configurations")
public class RateLimitConfigController {
    private final RateLimitConfigService service;

    @GetMapping
    public ResponseEntity<ApiResponse<List<RateLimitConfigResponse>>> getAllConfigs() {
        List<RateLimitConfigResponse> configs = service.getAllRateLimitConfig();

        ApiResponse<List<RateLimitConfigResponse>> response = ApiResponse.<List<RateLimitConfigResponse>>builder()
                .status(HttpStatus.OK.value())
                .message("Get configs successfully!")
                .data(configs)
                .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{config_id}")
    public ResponseEntity<ApiResponse<RateLimitConfigResponse>> getConfigById(@PathVariable Long config_id) {
        RateLimitConfigResponse config = service.getRateLimitConfigById(config_id);

        ApiResponse<RateLimitConfigResponse> response = ApiResponse.<RateLimitConfigResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Get config successfully!")
                .data(config)
                .build();

        return ResponseEntity.ok(response);
    }

    @PostMapping()
    public ResponseEntity<ApiResponse<RateLimitConfigResponse>> createConfig(

            @Valid @RequestBody RateLimitConfigCreateRequest request
    ){
        RateLimitConfigResponse config = service.createRateLimitConfig(
                request.getName(),
                request.getEndpointPattern(),
                request.getLimitScope(),
                request.getMaxRequest(),
                request.getTimeWindowSeconds(),
                request.getActionOnExceed(),
                request.getPriority()
        );

        ApiResponse<RateLimitConfigResponse> response = ApiResponse.<RateLimitConfigResponse>builder()
                .status(HttpStatus.CREATED.value())
                .message("Create config successfully!")
                .data(config)
                .build();

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{config_id}")
    public ResponseEntity<ApiResponse<RateLimitConfigResponse>> updateConfig(
        @PathVariable long config_id,
        @Valid @RequestBody RateLimitConfigUpdateRequest request
    ){
        RateLimitConfigResponse config = service.updateRateLimitConfig(
                config_id,
                request.getName(),
                request.getEndpointPattern(),
                request.getLimitScope(),
                request.getMaxRequest(),
                request.getTimeWindowSeconds(),
                request.getActionOnExceed(),
                request.getPriority(),
                request.getIsActive()
        );

        ApiResponse<RateLimitConfigResponse> response = ApiResponse.<RateLimitConfigResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Update config successfully!")
                .data(config)
                .build();

        return ResponseEntity.ok(response);
    }
}
