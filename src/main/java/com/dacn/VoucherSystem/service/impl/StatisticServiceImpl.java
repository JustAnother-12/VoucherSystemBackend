package com.dacn.VoucherSystem.service.impl;

import com.dacn.VoucherSystem.dto.response.*;
import com.dacn.VoucherSystem.entity.Campaign;
import com.dacn.VoucherSystem.entity.Voucher;
import com.dacn.VoucherSystem.entity.VoucherBatch;
import com.dacn.VoucherSystem.enums.BatchType;
import com.dacn.VoucherSystem.enums.TimeGranularity;
import com.dacn.VoucherSystem.exception.ResourceNotFoundException;
import com.dacn.VoucherSystem.repository.CampaignRepository;
import com.dacn.VoucherSystem.repository.DistributionLogRepository;
import com.dacn.VoucherSystem.repository.RedemptionLogRepository;
import com.dacn.VoucherSystem.repository.VoucherBatchRepository;
import com.dacn.VoucherSystem.repository.VoucherRepository;
import com.dacn.VoucherSystem.service.interfaces.StatisticService;
import com.dacn.VoucherSystem.util.StatisticUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StatisticServiceImpl implements StatisticService {

    private static final Duration HOUR_MAX_SPAN = Duration.ofDays(31);
    private static final Duration DAY_MAX_SPAN = Duration.ofDays(366);

    private final CampaignRepository campaignRepository;
    private final VoucherBatchRepository voucherBatchRepository;
    private final VoucherRepository voucherRepository;
    private final RedemptionLogRepository redemptionLogRepository;
    private final DistributionLogRepository distributionLogRepository;
    private final Clock clock;

    @Override
    public CampaignRedemptionResponse getCampaignRedemptionRate(
            Long campaignId,
            Long batchId,
            boolean includeCodes
    ) {
        Campaign campaign = findCampaign(campaignId);
        List<VoucherBatch> batches = findBatches(campaignId, batchId);

        long campaignIssued = 0;
        long campaignRedeemed = 0;
        long campaignDistributed = 0;
        List<BatchRedemptionResponse> batchResponses = new ArrayList<>();

        for (VoucherBatch batch : batches) {
            long batchIssued = batch.getMaxUsageCount() != null ? batch.getMaxUsageCount() : 0;
            long batchRedeemed = batch.getRedeemedCount() != null ? batch.getRedeemedCount() : 0;
            long batchDistributed = distributionLogRepository.countActiveByBatch(batch.getId());

            campaignIssued += batchIssued;
            campaignRedeemed += batchRedeemed;
            campaignDistributed += batchDistributed;

            batchResponses.add(BatchRedemptionResponse.builder()
                    .batchId(String.valueOf(batch.getId()))
                    .issuedCount(batchIssued)
                    .redeemedCount(batchRedeemed)
                    .redemptionRate(StatisticUtil.calculateRate(batchRedeemed, batchIssued))
                    .distributedCount(batchDistributed)
                    .distributedRedemptionRate(StatisticUtil.calculateRate(batchRedeemed, batchDistributed))
                    .codes(includeCodes ? buildCodeRedemptions(batch) : Collections.emptyList())
                    .build());
        }

        CampaignSummary summary = CampaignSummary.builder()
                .totalIssued(campaignIssued)
                .totalRedeemed(campaignRedeemed)
                .redemptionRate(StatisticUtil.calculateRate(campaignRedeemed, campaignIssued))
                .totalDistributed(campaignDistributed)
                .distributedRedemptionRate(StatisticUtil.calculateRate(campaignRedeemed, campaignDistributed))
                .build();

        return CampaignRedemptionResponse.builder()
                .campaignId(String.valueOf(campaign.getId()))
                .campaignName(campaign.getCampaignName())
                .campaignSummary(summary)
                .batches(batchResponses)
                .build();
    }

    private List<CodeRedemptionResponse> buildCodeRedemptions(VoucherBatch batch) {
        List<Voucher> codes = voucherRepository.findByBatchId(batch.getId());
        if (codes.isEmpty()) {
            return List.of();
        }

        List<Long> voucherIds = codes.stream().map(Voucher::getId).toList();
        Map<Long, Long> redeemedMap = redemptionLogRepository.countSuccessLogsByVoucherIds(voucherIds);
        Map<Long, Long> distributedMap = toLongMap(distributionLogRepository.countActiveByVoucherIds(voucherIds));

        long issuedPerCode = batch.getBatchType() == BatchType.GENERIC
                ? (batch.getMaxUsageCount() != null ? batch.getMaxUsageCount() : 1)
                : 1;

        return codes.stream().map(code -> {
            long redeemed = redeemedMap.getOrDefault(code.getId(), 0L);
            long distributed = distributedMap.getOrDefault(code.getId(), 0L);
            return CodeRedemptionResponse.builder()
                    .code(code.getCode())
                    .issuedCount(issuedPerCode)
                    .redeemedCount(redeemed)
                    .redemptionRate(StatisticUtil.calculateRate(redeemed, issuedPerCode))
                    .distributedCount(distributed)
                    .distributedRedemptionRate(StatisticUtil.calculateRate(redeemed, distributed))
                    .build();
        }).toList();
    }

    @Override
    public CampaignRevenueResponse getCampaignIncrementalRevenue(
            Long campaignId,
            Long batchId,
            boolean includeCodes,
            BigDecimal baselineRevenue
    ) {
        BigDecimal baseline = baselineRevenue != null ? baselineRevenue : BigDecimal.ZERO;
        if (baseline.signum() < 0) {
            throw new IllegalArgumentException("baselineRevenue không được âm");
        }

        Campaign campaign = findCampaign(campaignId);
        List<VoucherBatch> batches = findBatches(campaignId, batchId);

        Revenue campaignTotal = Revenue.ZERO;
        List<BatchRevenueResponse> batchResponses = new ArrayList<>();

        for (VoucherBatch batch : batches) {
            Revenue batchRevenue;
            List<CodeRevenueResponse> codeResponses = Collections.emptyList();

            if (includeCodes) {
                List<Voucher> codes = voucherRepository.findByBatchId(batch.getId());
                List<Long> voucherIds = codes.stream().map(Voucher::getId).toList();
                Map<Long, Object[]> revenueMap = voucherIds.isEmpty()
                        ? Map.of()
                        : redemptionLogRepository.aggregateRevenueByVoucherIds(voucherIds);

                batchRevenue = Revenue.ZERO;
                codeResponses = new ArrayList<>();
                for (Voucher code : codes) {
                    Revenue codeRevenue = Revenue.from(revenueMap.get(code.getId()));
                    batchRevenue = batchRevenue.plus(codeRevenue);
                    codeResponses.add(CodeRevenueResponse.builder()
                            .code(code.getCode())
                            .totalOriginalAmount(codeRevenue.original())
                            .totalDiscountAmount(codeRevenue.discount())
                            .totalFinalAmount(codeRevenue.finalAmount())
                            .roi(StatisticUtil.calculateRoi(codeRevenue.finalAmount(), codeRevenue.discount()))
                            .build());
                }
            } else {
                batchRevenue = Revenue.from(redemptionLogRepository.aggregateRevenueByBatchId(batch.getId()));
            }

            campaignTotal = campaignTotal.plus(batchRevenue);
            batchResponses.add(BatchRevenueResponse.builder()
                    .batchId(String.valueOf(batch.getId()))
                    .totalOriginalAmount(batchRevenue.original())
                    .totalDiscountAmount(batchRevenue.discount())
                    .totalFinalAmount(batchRevenue.finalAmount())
                    .roi(StatisticUtil.calculateRoi(batchRevenue.finalAmount(), batchRevenue.discount()))
                    .codes(codeResponses)
                    .build());
        }

        BigDecimal incremental = campaignTotal.finalAmount().subtract(baseline);

        CampaignRevenueSummary summary = CampaignRevenueSummary.builder()
                .totalOriginalAmount(campaignTotal.original())
                .totalDiscountAmount(campaignTotal.discount())
                .totalFinalAmount(campaignTotal.finalAmount())
                .baselineRevenue(baseline)
                .incrementalRevenue(incremental)
                .roi(StatisticUtil.calculateRoi(incremental, campaignTotal.discount()))
                .build();

        return CampaignRevenueResponse.builder()
                .campaignId(String.valueOf(campaign.getId()))
                .campaignName(campaign.getCampaignName())
                .campaignSummary(summary)
                .batches(batchResponses)
                .build();
    }

    @Override
    public DashboardOverviewResponse getDashboardOverview() {
        List<Campaign> campaigns = campaignRepository.findAll();
        List<VoucherBatch> batches = voucherBatchRepository.findAll();

        Map<Long, Long> distributedByCampaign =
                toLongMap(distributionLogRepository.countActiveGroupedByCampaign());
        Map<Long, Long> distributedByVoucher =
                toLongMap(distributionLogRepository.countActiveGroupedByVoucher());

        Map<Long, VoucherBatch> batchById = new HashMap<>();
        Map<Long, RevenueAcc> campaignAcc = new HashMap<>();

        for (VoucherBatch batch : batches) {
            batchById.put(batch.getId(), batch);
            RevenueAcc acc = campaignAcc.computeIfAbsent(batch.getCampaign().getId(), id -> new RevenueAcc());
            acc.issued += batch.getMaxUsageCount() != null ? batch.getMaxUsageCount() : 0;
            acc.redeemed += batch.getRedeemedCount() != null ? batch.getRedeemedCount() : 0;
        }

        List<DashboardOverviewResponse.CodeOverview> codes = new ArrayList<>();
        for (Object[] row : voucherRepository.findCodeOverviewRows()) {
            Long voucherId = (Long) row[0];
            String code = (String) row[1];
            Long batchId = (Long) row[2];
            Long campaignId = (Long) row[3];
            long redeemed = row[4] != null ? (Long) row[4] : 0L;

            VoucherBatch batch = batchById.get(batchId);
            long issued = (batch != null && batch.getBatchType() == BatchType.GENERIC)
                    ? (batch.getMaxUsageCount() != null ? batch.getMaxUsageCount() : 1)
                    : 1;
            long distributed = distributedByVoucher.getOrDefault(voucherId, 0L);

            BigDecimal original = nz((BigDecimal) row[5]);
            BigDecimal discount = nz((BigDecimal) row[6]);
            BigDecimal finalAmount = nz((BigDecimal) row[7]);

            codes.add(new DashboardOverviewResponse.CodeOverview(
                    String.valueOf(campaignId),
                    String.valueOf(batchId),
                    code,
                    issued,
                    redeemed,
                    StatisticUtil.calculateRate(redeemed, issued),
                    distributed,
                    StatisticUtil.calculateRate(redeemed, distributed),
                    original,
                    discount,
                    finalAmount,
                    StatisticUtil.calculateRoi(finalAmount, discount)
            ));

            RevenueAcc acc = campaignAcc.computeIfAbsent(campaignId, id -> new RevenueAcc());
            acc.original = acc.original.add(original);
            acc.discount = acc.discount.add(discount);
            acc.finalAmount = acc.finalAmount.add(finalAmount);
        }

        List<DashboardOverviewResponse.CampaignOverview> campaignOverviews = new ArrayList<>();
        RevenueAcc total = new RevenueAcc();
        for (Campaign campaign : campaigns) {
            RevenueAcc acc = campaignAcc.getOrDefault(campaign.getId(), new RevenueAcc());
            acc.distributed = distributedByCampaign.getOrDefault(campaign.getId(), 0L);
            total.add(acc);
            campaignOverviews.add(new DashboardOverviewResponse.CampaignOverview(
                    String.valueOf(campaign.getId()),
                    campaign.getCampaignName(),
                    acc.issued,
                    acc.redeemed,
                    StatisticUtil.calculateRate(acc.redeemed, acc.issued),
                    acc.distributed,
                    StatisticUtil.calculateRate(acc.redeemed, acc.distributed),
                    acc.original,
                    acc.discount,
                    acc.finalAmount,
                    StatisticUtil.calculateRoi(acc.finalAmount, acc.discount)
            ));
        }

        DashboardOverviewResponse.OverviewSummary summary = new DashboardOverviewResponse.OverviewSummary(
                total.issued,
                total.redeemed,
                StatisticUtil.calculateRate(total.redeemed, total.issued),
                total.distributed,
                StatisticUtil.calculateRate(total.redeemed, total.distributed),
                total.original,
                total.discount,
                total.finalAmount,
                StatisticUtil.calculateRoi(total.finalAmount, total.discount)
        );

        return new DashboardOverviewResponse(summary, campaignOverviews, codes);
    }

    @Override
    public RevenueTimeSeriesResponse getRevenueTimeSeries(
            TimeGranularity granularity,
            LocalDateTime from,
            LocalDateTime to,
            Long campaignId
    ) {
        TimeGranularity unit = granularity != null ? granularity : TimeGranularity.DAY;
        LocalDateTime end = to != null ? to : LocalDateTime.now(clock);
        LocalDateTime start = from != null
                ? from
                : end.minus(unit == TimeGranularity.HOUR ? Duration.ofHours(24) : Duration.ofDays(30));

        if (!start.isBefore(end)) {
            throw new IllegalArgumentException("from phải nhỏ hơn to");
        }
        Duration maxSpan = unit == TimeGranularity.HOUR ? HOUR_MAX_SPAN : DAY_MAX_SPAN;
        if (Duration.between(start, end).compareTo(maxSpan) > 0) {
            throw new IllegalArgumentException("Khoảng thời gian vượt giới hạn " + maxSpan.toDays()
                    + " ngày cho granularity " + unit);
        }

        if (campaignId != null) {
            findCampaign(campaignId);
        }

        Map<LocalDateTime, Object[]> rows = new HashMap<>();
        for (Object[] row : redemptionLogRepository.findRevenueTimeSeries(unit.sqlUnit(), start, end, campaignId)) {
            rows.put(toLocalDateTime(row[0]), row);
        }

        List<RevenueTimeSeriesResponse.Point> points = new ArrayList<>();
        for (LocalDateTime bucket = truncate(start, unit); bucket.isBefore(end); bucket = next(bucket, unit)) {
            Object[] row = rows.get(bucket);
            points.add(new RevenueTimeSeriesResponse.Point(
                    bucket,
                    row != null ? ((Number) row[1]).longValue() : 0L,
                    row != null ? toBigDecimal(row[2]) : BigDecimal.ZERO,
                    row != null ? toBigDecimal(row[3]) : BigDecimal.ZERO,
                    row != null ? toBigDecimal(row[4]) : BigDecimal.ZERO
            ));
        }

        return new RevenueTimeSeriesResponse(
                unit.name(),
                start,
                end,
                campaignId != null ? String.valueOf(campaignId) : null,
                points
        );
    }

    private Campaign findCampaign(Long campaignId) {
        return campaignRepository.findById(campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign not found with ID: " + campaignId));
    }

    private List<VoucherBatch> findBatches(Long campaignId, Long batchId) {
        if (batchId == null) {
            return voucherBatchRepository.findByCampaignId(campaignId);
        }
        VoucherBatch batch = voucherBatchRepository.findByIdAndCampaignId(batchId, campaignId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format("Voucher batch (Batch ID: %d) not found for Campaign ID: %d", batchId, campaignId)
                ));
        return List.of(batch);
    }

    private static Map<Long, Long> toLongMap(List<Object[]> rows) {
        Map<Long, Long> result = new HashMap<>();
        for (Object[] row : rows) {
            result.put((Long) row[0], ((Number) row[1]).longValue());
        }
        return result;
    }

    private static LocalDateTime truncate(LocalDateTime time, TimeGranularity unit) {
        return unit == TimeGranularity.HOUR
                ? time.truncatedTo(ChronoUnit.HOURS)
                : time.truncatedTo(ChronoUnit.DAYS);
    }

    private static LocalDateTime next(LocalDateTime time, TimeGranularity unit) {
        return unit == TimeGranularity.HOUR ? time.plusHours(1) : time.plusDays(1);
    }

    private static LocalDateTime toLocalDateTime(Object value) {
        if (value instanceof Timestamp ts) {
            return ts.toLocalDateTime();
        }
        return (LocalDateTime) value;
    }

    private static BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal bd) {
            return bd;
        }
        return new BigDecimal(value.toString());
    }

    private static BigDecimal nz(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private record Revenue(BigDecimal original, BigDecimal discount, BigDecimal finalAmount) {

        static final Revenue ZERO = new Revenue(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);

        static Revenue from(Object[] row) {
            if (row == null) {
                return ZERO;
            }
            return new Revenue(toBigDecimal(row[0]), toBigDecimal(row[1]), toBigDecimal(row[2]));
        }

        Revenue plus(Revenue other) {
            return new Revenue(
                    original.add(other.original),
                    discount.add(other.discount),
                    finalAmount.add(other.finalAmount)
            );
        }
    }

    private static final class RevenueAcc {
        long issued;
        long redeemed;
        long distributed;
        BigDecimal original = BigDecimal.ZERO;
        BigDecimal discount = BigDecimal.ZERO;
        BigDecimal finalAmount = BigDecimal.ZERO;

        void add(RevenueAcc other) {
            this.issued += other.issued;
            this.redeemed += other.redeemed;
            this.distributed += other.distributed;
            this.original = this.original.add(other.original);
            this.discount = this.discount.add(other.discount);
            this.finalAmount = this.finalAmount.add(other.finalAmount);
        }
    }
}