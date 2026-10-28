package com.dacn.VoucherSystem.controller;

import com.dacn.VoucherSystem.dto.request.RewardCreateRequest;
import com.dacn.VoucherSystem.dto.request.RewardUpdateRequest;
import com.dacn.VoucherSystem.dto.response.ApiResponse;
import com.dacn.VoucherSystem.dto.response.RewardResponse;
import com.dacn.VoucherSystem.service.RewardService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/rule/rewards")
@Tag(name = "Rule Engine", description = "Operations pertaining to rules, rewards, and conditions")
public class RewardController {
    private final RewardService service;

    @GetMapping
    public ResponseEntity<ApiResponse<List<RewardResponse>>> getAllConfigs() {
        List<RewardResponse> rewards = service.getAllReward();

        ApiResponse<List<RewardResponse>> response = ApiResponse.<List<RewardResponse>>builder()
                .status(HttpStatus.OK.value())
                .message("Get rewards successfully!")
                .data(rewards)
                .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RewardResponse>> getConfigById(@PathVariable Long id) {
        RewardResponse reward = service.getRewardById(id);

        ApiResponse<RewardResponse> response = ApiResponse.<RewardResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Get reward successfully!")
                .data(reward)
                .build();

        return ResponseEntity.ok(response);
    }

    @PostMapping()
    public ResponseEntity<ApiResponse<RewardResponse>> createConfig(
            @Valid @RequestBody RewardCreateRequest request
    ){
        RewardResponse reward = service.createReward(
                request.getName(),
                request.getRewardType(),
                request.getConfig()
        );

        ApiResponse<RewardResponse> response = ApiResponse.<RewardResponse>builder()
                .status(HttpStatus.CREATED.value())
                .message("Create reward successfully!")
                .data(reward)
                .build();

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<RewardResponse>> updateConfig(
            @PathVariable long id,
            @Valid @RequestBody RewardUpdateRequest request
    ){
        RewardResponse reward = service.updateReward(
                id,
                request.getName(),
                request.getRewardType(),
                request.getConfig()
        );

        ApiResponse<RewardResponse> response = ApiResponse.<RewardResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Update reward successfully!")
                .data(reward)
                .build();

        return ResponseEntity.ok(response);
    }
}
