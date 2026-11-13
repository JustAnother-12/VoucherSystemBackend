package com.dacn.VoucherSystem.service.interfaces;

import com.dacn.VoucherSystem.dto.response.CampaignRedemptionResponse;
import com.dacn.VoucherSystem.dto.response.CampaignRevenueResponse;
import com.dacn.VoucherSystem.dto.response.DashboardOverviewResponse;
import com.dacn.VoucherSystem.dto.response.RevenueTimeSeriesResponse;
import com.dacn.VoucherSystem.enums.TimeGranularity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface StatisticService {

    CampaignRedemptionResponse getCampaignRedemptionRate(
            Long campaignId,
            Long batchId,
            boolean includeCodes
    );

    CampaignRevenueResponse getCampaignIncrementalRevenue(
            Long campaignId,
            Long batchId,
            boolean includeCodes,
            BigDecimal baselineRevenue
    );

    DashboardOverviewResponse getDashboardOverview();

    RevenueTimeSeriesResponse getRevenueTimeSeries(
            TimeGranularity granularity,
            LocalDateTime from,
            LocalDateTime to,
            Long campaignId
    );
}