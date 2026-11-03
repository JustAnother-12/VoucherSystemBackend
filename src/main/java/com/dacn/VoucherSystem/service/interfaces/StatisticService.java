package com.dacn.VoucherSystem.service.interfaces;

import com.dacn.VoucherSystem.dto.response.CampaignRedemptionResponse;
import com.dacn.VoucherSystem.dto.response.CampaignRevenueResponse;

public interface StatisticService {
    CampaignRedemptionResponse getCampaignRedemptionRate(
            String campaignId,
            String batchId,
            boolean includeCodes
    );
    CampaignRevenueResponse getCampaignIncrementalRevenue(
            String campaignId,
            String batchId,
            boolean includeCodes
    );
}
