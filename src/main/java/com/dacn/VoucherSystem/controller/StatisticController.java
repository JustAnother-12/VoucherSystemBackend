package com.dacn.VoucherSystem.controller;

import com.dacn.VoucherSystem.dto.response.ApiResponse;
import com.dacn.VoucherSystem.dto.response.CampaignRedemptionResponse;
import com.dacn.VoucherSystem.dto.response.CampaignRevenueResponse;
import com.dacn.VoucherSystem.service.interfaces.StatisticService;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/statistic")
@Tag(name = "Statistic", description = "Operations pertaining to Statistic")
public class StatisticController {
    private final StatisticService statisticService;

    @GetMapping("/campaigns/{campaignId}/redemption-rate")
    public ResponseEntity<ApiResponse<CampaignRedemptionResponse>> getCampaignRedemptionRate(
            @PathVariable Long campaignId,
            @RequestParam(required = false) Long batchId,
            @RequestParam(defaultValue = "false") boolean includeCodes
    ) {
        CampaignRedemptionResponse data = statisticService.getCampaignRedemptionRate(
                campaignId,
                batchId,
                includeCodes
        );

        ApiResponse<CampaignRedemptionResponse> response = ApiResponse.<CampaignRedemptionResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Get campaign redemption rate statistics successfully!")
                .data(data)
                .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/campaigns/{campaignId}/incremental-revenue")
    public ResponseEntity<ApiResponse<CampaignRevenueResponse>> getCampaignIncrementalRevenue(
            @PathVariable Long campaignId,
            @RequestParam(required = false) Long batchId,
            @RequestParam(defaultValue = "false") boolean includeCodes
    ) {
        CampaignRevenueResponse data = statisticService.getCampaignIncrementalRevenue(
                campaignId,
                batchId,
                includeCodes
        );

        ApiResponse<CampaignRevenueResponse> response = ApiResponse.<CampaignRevenueResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Get campaign incremental revenue statistics successfully!")
                .data(data)
                .build();

        return ResponseEntity.ok(response);
    }
}
