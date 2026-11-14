package com.dacn.VoucherSystem.controller;

import com.dacn.VoucherSystem.dto.response.ApiResponse;
import com.dacn.VoucherSystem.dto.response.CampaignRedemptionResponse;
import com.dacn.VoucherSystem.dto.response.CampaignRevenueResponse;
import com.dacn.VoucherSystem.dto.response.DashboardOverviewResponse;
import com.dacn.VoucherSystem.dto.response.RevenueTimeSeriesResponse;
import com.dacn.VoucherSystem.enums.TimeGranularity;
import com.dacn.VoucherSystem.service.interfaces.StatisticService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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
            @RequestParam(defaultValue = "false") boolean includeCodes,
            @RequestParam(required = false) BigDecimal baselineRevenue
    ) {
        CampaignRevenueResponse data = statisticService.getCampaignIncrementalRevenue(
                campaignId,
                batchId,
                includeCodes,
                baselineRevenue
        );

        ApiResponse<CampaignRevenueResponse> response = ApiResponse.<CampaignRevenueResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Get campaign incremental revenue statistics successfully!")
                .data(data)
                .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/overview")
    public ResponseEntity<ApiResponse<DashboardOverviewResponse>> getDashboardOverview() {
        DashboardOverviewResponse data = statisticService.getDashboardOverview();

        ApiResponse<DashboardOverviewResponse> response = ApiResponse.<DashboardOverviewResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Get dashboard overview successfully!")
                .data(data)
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Doanh thu theo thời gian để vẽ biểu đồ.
     * Ví dụ: /statistic/timeseries?granularity=DAY&from=2026-11-01T00:00:00&to=2026-11-08T00:00:00&campaignId=1
     * Mặc định: DAY, 30 ngày gần nhất (HOUR: 24 giờ gần nhất).
     */
    @GetMapping("/timeseries")
    public ResponseEntity<ApiResponse<RevenueTimeSeriesResponse>> getRevenueTimeSeries(
            @RequestParam(defaultValue = "DAY") TimeGranularity granularity,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(required = false) Long campaignId
    ) {
        RevenueTimeSeriesResponse data = statisticService.getRevenueTimeSeries(
                granularity,
                from,
                to,
                campaignId
        );

        ApiResponse<RevenueTimeSeriesResponse> response = ApiResponse.<RevenueTimeSeriesResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Get revenue time series successfully!")
                .data(data)
                .build();

        return ResponseEntity.ok(response);
    }
}