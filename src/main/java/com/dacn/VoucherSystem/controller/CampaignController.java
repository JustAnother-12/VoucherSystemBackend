package com.dacn.VoucherSystem.controller;

import com.dacn.VoucherSystem.dto.request.CampaignCreateRequest;
import com.dacn.VoucherSystem.dto.request.CampaignUpdateRequest;
import com.dacn.VoucherSystem.dto.response.ApiResponse;
import com.dacn.VoucherSystem.dto.response.CampaignResponse;
import com.dacn.VoucherSystem.security.CustomUserDetails;
import com.dacn.VoucherSystem.service.interfaces.CampaignService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/campaigns")
@Tag(name = "Campaigns", description = "Operations pertaining to Campaigns")
public class CampaignController {
    private final CampaignService service;

    @GetMapping
    public ResponseEntity<ApiResponse<List<CampaignResponse>>> getAllCampaigns() {
        List<CampaignResponse> campaigns = service.getAllCampaign();

        ApiResponse<List<CampaignResponse>> response = ApiResponse.<List<CampaignResponse>>builder()
                .status(HttpStatus.OK.value())
                .message("Get campaigns successfully!")
                .data(campaigns)
                .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CampaignResponse>> getCampaignById(@PathVariable Long id) {
        CampaignResponse campaign = service.getCampaignByID(id);

        ApiResponse<CampaignResponse> response = ApiResponse.<CampaignResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Get campaign successfully!")
                .data(campaign)
                .build();

        return ResponseEntity.ok(response);
    }

    @PostMapping()
    public ResponseEntity<ApiResponse<CampaignResponse>> createConfig(
            @Valid @RequestBody CampaignCreateRequest request,
            @AuthenticationPrincipal CustomUserDetails user
    ){
        CampaignResponse campaign = service.createCampaign(
                request.name(),
                user.getAdminAccount(),
                request.startTime(),
                request.endTime()
        );

        ApiResponse<CampaignResponse> response = ApiResponse.<CampaignResponse>builder()
                .status(HttpStatus.CREATED.value())
                .message("Create campaign successfully!")
                .data(campaign)
                .build();

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<CampaignResponse>> updateConfig(
            @PathVariable long id,
            @Valid @RequestBody CampaignUpdateRequest request
    ){
        CampaignResponse campaign = service.updateCampaign(
                id,
                request.name(),
                request.startTime(),
                request.endTime()
        );

        ApiResponse<CampaignResponse> response = ApiResponse.<CampaignResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Update condition successfully!")
                .data(campaign)
                .build();

        return ResponseEntity.ok(response);
    }
}
