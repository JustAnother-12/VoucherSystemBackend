package com.dacn.VoucherSystem.service.impl;

import com.dacn.VoucherSystem.dto.response.*;
import com.dacn.VoucherSystem.entity.Campaign;
import com.dacn.VoucherSystem.entity.Voucher;
import com.dacn.VoucherSystem.entity.VoucherBatch;
import com.dacn.VoucherSystem.enums.BatchType;
import com.dacn.VoucherSystem.exception.ResourceNotFoundException;
import com.dacn.VoucherSystem.repository.CampaignRepository;
import com.dacn.VoucherSystem.repository.RedemptionLogRepository;
import com.dacn.VoucherSystem.repository.VoucherBatchRepository;
import com.dacn.VoucherSystem.repository.VoucherRepository;
import com.dacn.VoucherSystem.service.interfaces.StatisticService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.dacn.VoucherSystem.util.StatisticUtil;

import java.math.BigDecimal;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StatisticServiceImpl implements StatisticService {
    private final CampaignRepository campaignRepository;
    private final VoucherBatchRepository voucherBatchRepository;
    private final VoucherRepository voucherRepository;
    private final RedemptionLogRepository redemptionLogRepository;

    @Override
    public CampaignRedemptionResponse getCampaignRedemptionRate(
            Long campaignId,
            Long batchId,
            boolean includeCodes
    ) {
        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign not found with ID: " + campaignId));

        List<VoucherBatch> batches;
        if (batchId != null) {
            VoucherBatch batch = voucherBatchRepository.findByIdAndCampaignId(batchId, campaignId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            String.format("Voucher batch (Batch ID: %d) not found for Campaign ID: %d", batchId, campaignId)
                    ));
            batches = List.of(batch);
        } else {
            batches = voucherBatchRepository.findByCampaignId(campaignId);
        }

        long campaignTotalIssued = 0;
        long campaignTotalRedeemed = 0;

        List<BatchRedemptionResponse> batchResponses = batches.stream().map(batch -> {
            long batchIssued = batch.getGeneratedCount() != null ? batch.getGeneratedCount() : 0;
            long batchRedeemed = batch.getRedeemedCount() != null ? batch.getRedeemedCount() : 0;
            double batchRate = StatisticUtil.calculateRate(batchRedeemed, batchIssued);
            List<CodeRedemptionResponse> codeResponses = Collections.emptyList();

            if (includeCodes) {
                List<Voucher> codes = voucherRepository.findByBatchId(batch.getId());
                List<Long> voucherIds = codes.stream().map(Voucher::getId).toList();

                Map<Long, Long> successCountMap = voucherIds.isEmpty() ? Collections.emptyMap()
                        : redemptionLogRepository.countSuccessLogsByVoucherIds(voucherIds);

                codeResponses = codes.stream().map(code -> {
                    long codeRedeemed = successCountMap.getOrDefault(code.getId(), 0L);
                    long codeIssued = (batch.getBatchType() == BatchType.GENERIC)
                            ? (batch.getGeneratedCount() != null ? batch.getGeneratedCount() : 1)
                            : 1;

                    double codeRate = StatisticUtil.calculateRate(codeRedeemed, codeIssued);

                    return CodeRedemptionResponse.builder()
                            .code(code.getCode())
                            .issuedCount(codeIssued)
                            .redeemedCount(codeRedeemed)
                            .redemptionRate(codeRate)
                            .build();
                }).toList();
            }

            return BatchRedemptionResponse.builder()
                    .batchId(String.valueOf(batch.getId()))
                    .issuedCount(batchIssued)
                    .redeemedCount(batchRedeemed)
                    .redemptionRate(batchRate)
                    .codes(codeResponses)
                    .build();
        }).toList();

        for (BatchRedemptionResponse batchResp : batchResponses) {
            campaignTotalIssued += batchResp.issuedCount();
            campaignTotalRedeemed += batchResp.redeemedCount();
        }
        double campaignRate = StatisticUtil.calculateRate(campaignTotalRedeemed, campaignTotalIssued);

        CampaignSummary summary = CampaignSummary.builder()
                .totalIssued(campaignTotalIssued)
                .totalRedeemed(campaignTotalRedeemed)
                .redemptionRate(campaignRate)
                .build();

        return CampaignRedemptionResponse.builder()
                .campaignId(String.valueOf(campaign.getId()))
                .campaignName(campaign.getCampaignName())
                .campaignSummary(summary)
                .batches(batchResponses)
                .build();
    }

    @Override
    public CampaignRevenueResponse getCampaignIncrementalRevenue(
            Long campaignId,
            Long batchId,
            boolean includeCodes
    ) {
        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign not found with ID: " + campaignId));

        List<VoucherBatch> batches;
        if (batchId != null) {
            VoucherBatch batch = voucherBatchRepository.findByIdAndCampaignId(batchId, campaignId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            String.format("Voucher batch (Batch ID: %d) not found for Campaign ID: %d", batchId, campaignId)
                    ));
            batches = List.of(batch);
        } else {
            batches = voucherBatchRepository.findByCampaignId(campaignId);
        }

        BigDecimal campaignTotalOriginal = BigDecimal.ZERO;
        BigDecimal campaignTotalDiscount = BigDecimal.ZERO;
        BigDecimal campaignTotalFinal = BigDecimal.ZERO;

        List<BatchRevenueResponse> batchResponses = batches.stream().map(batch -> {
            BigDecimal batchOriginal = BigDecimal.ZERO;
            BigDecimal batchDiscount = BigDecimal.ZERO;
            BigDecimal batchFinal = BigDecimal.ZERO;
            List<CodeRevenueResponse> codeResponses = Collections.emptyList();

            if (includeCodes) {
                List<Voucher> codes = voucherRepository.findByBatchId(batch.getId());
                List<Long> voucherIds = codes.stream().map(Voucher::getId).toList();

                Map<Long, Object[]> revenueMap = voucherIds.isEmpty() ? Collections.emptyMap()
                        : redemptionLogRepository.aggregateRevenueByVoucherIds(voucherIds);

                for (Object[] row : revenueMap.values()) {
                    if (row[0] != null) batchOriginal = batchOriginal.add((BigDecimal) row[0]);
                    if (row[1] != null) batchDiscount = batchDiscount.add((BigDecimal) row[1]);
                    if (row[2] != null) batchFinal = batchFinal.add((BigDecimal) row[2]);
                }

                codeResponses = codes.stream().map(code -> {
                    Object[] row = revenueMap.get(code.getId());

                    BigDecimal codeOriginal = (row != null && row[0] != null) ? (BigDecimal) row[0] : BigDecimal.ZERO;
                    BigDecimal codeDiscount = (row != null && row[1] != null) ? (BigDecimal) row[1] : BigDecimal.ZERO;
                    BigDecimal codeFinal    = (row != null && row[2] != null) ? (BigDecimal) row[2] : BigDecimal.ZERO;

                    double codeRoi = StatisticUtil.calculateRoi(codeFinal, codeDiscount);

                    return CodeRevenueResponse.builder()
                            .code(code.getCode())
                            .totalOriginalAmount(codeOriginal)
                            .totalDiscountAmount(codeDiscount)
                            .totalFinalAmount(codeFinal)
                            .roi(codeRoi)
                            .build();
                }).toList();
            } else {
                Object[] batchRevenue = redemptionLogRepository.aggregateRevenueByBatchId(batch.getId());
                if (batchRevenue != null) {
                    if (batchRevenue[0] != null) batchOriginal = (BigDecimal) batchRevenue[0];
                    if (batchRevenue[1] != null) batchDiscount = (BigDecimal) batchRevenue[1];
                    if (batchRevenue[2] != null) batchFinal    = (BigDecimal) batchRevenue[2];
                }
            }

            double batchRoi = StatisticUtil.calculateRoi(batchFinal, batchDiscount);

            return BatchRevenueResponse.builder()
                    .batchId(String.valueOf(batch.getId()))
                    .totalOriginalAmount(batchOriginal)
                    .totalDiscountAmount(batchDiscount)
                    .totalFinalAmount(batchFinal)
                    .roi(batchRoi)
                    .codes(codeResponses)
                    .build();
        }).toList();

        for (BatchRevenueResponse b : batchResponses) {
            campaignTotalOriginal = campaignTotalOriginal.add(b.totalOriginalAmount());
            campaignTotalDiscount = campaignTotalDiscount.add(b.totalDiscountAmount());
            campaignTotalFinal = campaignTotalFinal.add(b.totalFinalAmount());
        }

        double campaignRoi = StatisticUtil.calculateRoi(campaignTotalFinal, campaignTotalDiscount);

        CampaignRevenueSummary summary = CampaignRevenueSummary.builder()
                .totalOriginalAmount(campaignTotalOriginal)
                .totalDiscountAmount(campaignTotalDiscount)
                .totalFinalAmount(campaignTotalFinal)
                .roi(campaignRoi)
                .build();

        return CampaignRevenueResponse.builder()
                .campaignId(String.valueOf(campaign.getId()))
                .campaignName(campaign.getCampaignName())
                .campaignSummary(summary)
                .batches(batchResponses)
                .build();
    }
}
