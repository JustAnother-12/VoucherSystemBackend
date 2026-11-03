package com.dacn.VoucherSystem.service.interfaces;

import com.dacn.VoucherSystem.dto.response.CampaignRedemptionResponse;
import com.dacn.VoucherSystem.dto.response.CampaignRevenueResponse;

public interface StatisticService {
    CampaignRedemptionResponse getCampaignRedemptionRate(
            Long campaignId,
            Long batchId,
            boolean includeCodes
    );
    CampaignRevenueResponse getCampaignIncrementalRevenue(
            Long campaignId,
            Long batchId,
            boolean includeCodes
    );
}
