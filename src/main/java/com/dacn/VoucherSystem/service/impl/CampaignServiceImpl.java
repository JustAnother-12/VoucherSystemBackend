package com.dacn.VoucherSystem.service.impl;

import com.dacn.VoucherSystem.dto.response.CampaignResponse;
import com.dacn.VoucherSystem.entity.AdminAccount;
import com.dacn.VoucherSystem.entity.Campaign;
import com.dacn.VoucherSystem.enums.CampaignStatus;
import com.dacn.VoucherSystem.exception.InvalidCampaignException;
import com.dacn.VoucherSystem.exception.ResourceNotFoundException;
import com.dacn.VoucherSystem.mapper.CampaignMapper;
import com.dacn.VoucherSystem.repository.CampaignRepository;
import com.dacn.VoucherSystem.service.interfaces.CampaignService;
import lombok.RequiredArgsConstructor;
import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CampaignServiceImpl implements CampaignService {
    private final CampaignRepository campaignRepository;
    private final CampaignMapper campaignMapper;

    @Override
    public List<CampaignResponse> getAllCampaign(){
        List<Campaign> campaigns = campaignRepository.findAll();

        return campaigns.stream()
                .map(campaignMapper::toCampaignResponse)
                .toList();
    }

    @Override
    public CampaignResponse getCampaignByID(Long id){
        Campaign campaign = campaignRepository.findById(id)
                .orElseThrow(
                        () -> new ResourceNotFoundException("No Campaign with id " + id + " found in the system")
                );

        return campaignMapper.toCampaignResponse(campaign);
    }

    @Override
    @Transactional
    public CampaignResponse createCampaign(
            String name,
            AdminAccount adminAccount,
            LocalDateTime startTime,
            LocalDateTime endTime
    ){
        if (!endTime.isAfter(startTime)){
            throw new InvalidCampaignException("endTime must after starTime");
        }

        Campaign campaign = Campaign.builder()
                .campaignName(name)
                .startTime(startTime)
                .endTime(endTime)
                .admin(adminAccount)
                .voucherBatches(new ArrayList<>())
                .status(CampaignStatus.DRAFT)
                .build();

        campaignRepository.saveAndFlush(campaign);

        return campaignMapper.toCampaignResponse(campaign);
    }

    @Override
    @Transactional
    public CampaignResponse updateCampaign(
            Long id,
            JsonNullable<String> name,
            JsonNullable<LocalDateTime> startTime,
            JsonNullable<LocalDateTime> endTime
    ){
        Campaign campaign = campaignRepository.findById(id)
                .orElseThrow(
                        () -> new ResourceNotFoundException("No Campaign with id " + id + " found in the system"));

        if (name.isPresent()) {
            String newName = name.get();
            if (newName == null || newName.isBlank()) {
                throw new InvalidCampaignException("name must not be blank");
            }
            campaign.setCampaignName(newName);
        }

        LocalDateTime newStart = startTime.isPresent() ? startTime.get() : campaign.getStartTime();
        LocalDateTime newEnd = endTime.isPresent() ? endTime.get() : campaign.getEndTime();

        if (newStart == null || newEnd == null) {
            throw new InvalidCampaignException("startTime and endTime must not be null");
        }

        if (!newEnd.isAfter(newStart)) {
            throw new InvalidCampaignException("endTime must be after startTime");
        }

        campaign.setStartTime(newStart);
        campaign.setEndTime(newEnd);

        campaignRepository.save(campaign);

        return campaignMapper.toCampaignResponse(campaign);
    }


}
