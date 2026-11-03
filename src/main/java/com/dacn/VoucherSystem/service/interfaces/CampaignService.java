package com.dacn.VoucherSystem.service.interfaces;

import com.dacn.VoucherSystem.dto.response.CampaignResponse;
import com.dacn.VoucherSystem.entity.AdminAccount;
import org.openapitools.jackson.nullable.JsonNullable;

import java.time.LocalDateTime;
import java.util.List;

public interface CampaignService {
    List<CampaignResponse> getAllCampaign();
    CampaignResponse getCampaignByID(Long id);
    CampaignResponse createCampaign(
            String name,
            AdminAccount adminAccount,
            LocalDateTime startTime,
            LocalDateTime endTime
    );
    CampaignResponse updateCampaign(
            Long id,
            JsonNullable<String> name,
            JsonNullable<LocalDateTime> startTime,
            JsonNullable<LocalDateTime> endTime
    );
}
