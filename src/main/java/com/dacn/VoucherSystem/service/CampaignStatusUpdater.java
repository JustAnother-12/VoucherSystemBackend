package com.dacn.VoucherSystem.service;

import com.dacn.VoucherSystem.enums.CampaignStatus;
import com.dacn.VoucherSystem.event.VoucherSnapshotsChangedEvent;
import com.dacn.VoucherSystem.repository.CampaignRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CampaignStatusUpdater {
    private final CampaignRepository campaignRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;
//  Định kỳ 60 phút
    @Scheduled(fixedDelayString = "${campaign.status-update.fixed-delay:3600000}")  
    @Transactional
    public void updateCampaignStatuses() {
        LocalDateTime now = LocalDateTime.now(clock);

        int activated = campaignRepository.activateCampaignsInTimeWindow(
                now,
                CampaignStatus.ACTIVE,
                CampaignStatus.PAUSED
        );
        int ended = campaignRepository.endExpiredCampaigns(
                now,
                CampaignStatus.ENDED,
                CampaignStatus.PAUSED
        );

        if (activated > 0 || ended > 0) {
            eventPublisher.publishEvent(new VoucherSnapshotsChangedEvent());
        }
    }
}
