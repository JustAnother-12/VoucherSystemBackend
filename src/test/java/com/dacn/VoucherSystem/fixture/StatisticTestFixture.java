package com.dacn.VoucherSystem.fixture;

import com.dacn.VoucherSystem.entity.*;
import com.dacn.VoucherSystem.enums.*;
import com.dacn.VoucherSystem.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class StatisticTestFixture {

    private final CampaignRepository campaignRepository;
    private final VoucherBatchRepository voucherBatchRepository;
    private final VoucherRepository voucherRepository;
    private final RedemptionLogRepository redemptionLogRepository;
    private final AdminAccountRepository adminAccountRepository;
    private final BatchRuleRepository batchRuleRepository;
    private final ConditionRepository conditionRepository;
    private final RewardRepository rewardRepository;
    public final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public record Fixture(
            Long campaignId,
            Long batchId,
            String campaignName,
            List<Voucher> savedVouchers
    ) {}

    @Transactional
    public void cleanDatabase() {
        redemptionLogRepository.deleteAllInBatch();
        voucherRepository.deleteAllInBatch();
        voucherBatchRepository.deleteAllInBatch();
        batchRuleRepository.deleteAllInBatch();
        conditionRepository.deleteAllInBatch();
        rewardRepository.deleteAllInBatch();
        campaignRepository.deleteAllInBatch();
        adminAccountRepository.deleteAllInBatch();
    }

    @Transactional
    public Fixture createFixture() {
        AdminAccount admin = adminAccountRepository.save(AdminAccount.builder()
                .createdAt(LocalDateTime.now())
                .username("it_admin_" + UUID.randomUUID())
                .hashedPassword("test-password")
                .isActive(true)
                .role(AdminRole.SYSTEM)
                .build());

        Campaign campaign = campaignRepository.save(Campaign.builder()
                .createdAt(LocalDateTime.now())
                .campaignName("Summer Festival Voucher")
                .startTime(LocalDateTime.now())
                .endTime(LocalDateTime.now().plusDays(30))
                .status(CampaignStatus.ACTIVE)
                .admin(admin)
                .build());

        Condition condition = conditionRepository.saveAndFlush(Condition.builder()
                .createdAt(LocalDateTime.now())
                .conditionName("Minimum cart amount")
                .conditionType(ConditionType.MIN_CART_AMOUNT)
                .conditionJson(
                        objectMapper.readTree("""
                            {
                              "minimumAmount": 50
                            }
                            """)
                )
                .status(RuleStatus.PUBLISHED)
                .build());

        Reward reward = rewardRepository.saveAndFlush(Reward.builder()
                .createdAt(LocalDateTime.now())
                .rewardName("10 percent discount")
                .rewardType(RewardType.PERCENTAGE)
                .rewardJson(
                        objectMapper.readTree("""
                            {
                              "discountPercent": 10,
                              "maxDiscountAmount": 500000
                            }
                            """)
                )
                .status(RuleStatus.PUBLISHED)
                .build());

        BatchRule rule = batchRuleRepository.saveAndFlush(BatchRule.builder()
                .createdAt(LocalDateTime.now())
                .condition(condition)
                .reward(reward)
                .build());

        VoucherBatch batch = voucherBatchRepository.save(VoucherBatch.builder()
                .createdAt(LocalDateTime.now())
                .campaign(campaign)
                .rule(rule)
                .batchType(BatchType.BULK)
                .codeConfigJson(
                        objectMapper.readTree("{}")
                )
                .distributionChannelsJson(
                        objectMapper.readTree("[]")
                )
                .durationUnit(DurationUnit.DAYS)
                .durationValue(7)
                .generatedCount(20)
                .redeemedCount(2)
                .status(BatchStatus.GENERATED)
                .build());

        List<Voucher> vouchers = new ArrayList<>();
        for (int i = 1; i <= 20; i++) {
            vouchers.add(Voucher.builder()
                    .createdAt(LocalDateTime.now())
                    .batch(batch)
                    .campaign(campaign)
                    .code("BULK_CODE_" + i)
                    .build());
        }
        List<Voucher> savedVouchers = voucherRepository.saveAll(vouchers);

        redemptionLogRepository.save(RedemptionLog.builder()
                .createdAt(LocalDateTime.now())
                .voucher(savedVouchers.get(0))
                .customerId("customer1")
                .ipAddress("172.0.0.1")
                .deviceFingerprint("FP-TEST-CUSTOMER-1")
                .originalAmount(new BigDecimal("1000000"))
                .discountAmount(new BigDecimal("100000"))
                .finalAmount(new BigDecimal("900000"))
                .status(RedemptionStatus.SUCCESS)
                .build());

        redemptionLogRepository.save(RedemptionLog.builder()
                .createdAt(LocalDateTime.now())
                .voucher(savedVouchers.get(1))
                .customerId("customer2")
                .ipAddress("172.0.0.1")
                .deviceFingerprint("FP-TEST-CUSTOMER-2")
                .originalAmount(new BigDecimal("300000"))
                .discountAmount(new BigDecimal("30000"))
                .finalAmount(new BigDecimal("270000"))
                .status(RedemptionStatus.SUCCESS)
                .build());

        return new Fixture(
                campaign.getId(),
                batch.getId(),
                campaign.getCampaignName(),
                savedVouchers
        );
    }

    public int countSuccessLogsByCampaign(Long campaignId) {
        return jdbcTemplate.queryForObject(
                """
                SELECT COUNT(r.id)
                FROM redemption_logs r
                JOIN vouchers v ON r.voucher_id = v.id
                JOIN voucher_batches b ON v.batch_id = b.id
                WHERE b.campaign_id = ? AND r.status = 'SUCCESS'
                """,
                Integer.class,
                campaignId
        );
    }
}
