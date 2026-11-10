package com.dacn.VoucherSystem.fixture;

import com.dacn.VoucherSystem.controller.RedemptionControllerIntegrationTest;
import com.dacn.VoucherSystem.entity.*;
import com.dacn.VoucherSystem.enums.*;
import com.dacn.VoucherSystem.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;

@Component
public class RedemptionTestFixture {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private AdminAccountRepository adminAccountRepository;

    @Autowired
    private CampaignRepository campaignRepository;

    @Autowired
    private ConditionRepository conditionRepository;

    @Autowired
    private RewardRepository rewardRepository;

    @Autowired
    private BatchRuleRepository batchRuleRepository;

    @Autowired
    private VoucherBatchRepository voucherBatchRepository;

    @Autowired
    private VoucherRepository voucherRepository;

    @Autowired
    private CustomerVoucherRepository customerVoucherRepository;

    @Autowired
    private RedemptionLogRepository redemptionLogRepository;

    @Autowired
    public Clock clock;

    @Autowired
    public JdbcTemplate jdbcTemplate;

    public record Fixture(
            Long voucherId,
            Long batchId,
            Long campaignId,
            String code
    ) {}

    public void cleanDatabase() {
        jdbcTemplate.execute("""
            TRUNCATE TABLE
                sessions,
                redemption_logs,
                fraud_logs,
                distribution_logs,
                customer_vouchers,
                vouchers,
                voucher_batch_redemption_counters,
                voucher_batches,
                batch_rules,
                rewards,
                conditions,
                campaigns,
                rate_limit_configs,
                black_lists,
                admin_accounts
            RESTART IDENTITY CASCADE
            """);
    }

    public Fixture createFixture(
            BatchType batchType,
            CampaignStatus campaignStatus
    ) throws Exception {
        return createFixture(batchType, campaignStatus, 10, 10);
    }

    public Fixture createFixture(
            BatchType batchType,
            CampaignStatus campaignStatus,
            int generatedCount,
            int maxUsageCount
    ) throws Exception {
        LocalDateTime now = LocalDateTime.now(clock).withNano(0);

        // 1. Admin account
        AdminAccount admin = AdminAccount.builder()
                .createdAt(LocalDateTime.now())
                .username("it_admin_" + UUID.randomUUID())
                .hashedPassword("test-password")
                .isActive(true)
                .role(AdminRole.SYSTEM)
                .build();

        admin = adminAccountRepository.saveAndFlush(admin);

        // 2. Campaign
        Campaign campaign = Campaign.builder()
                .createdAt(LocalDateTime.now())
                .campaignName("Integration Test Campaign")
                .startTime(now.minusDays(1))
                .endTime(now.plusDays(1))
                .status(campaignStatus)
                .admin(admin)
                .build();

        campaign = campaignRepository.saveAndFlush(campaign);

        // 3. Condition
        Condition condition = Condition.builder()
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
                .build();

        condition = conditionRepository.saveAndFlush(condition);

        // 4. Reward
        Reward reward = Reward.builder()
                .createdAt(LocalDateTime.now())
                .rewardName("10 percent discount")
                .rewardType(RewardType.PERCENTAGE)
                .rewardJson(
                        objectMapper.readTree("""
                            {
                              "discountPercent": 10,
                              "maxDiscountAmount": 50
                            }
                            """)
                )
                .status(RuleStatus.PUBLISHED)
                .build();

        reward = rewardRepository.saveAndFlush(reward);

        // 5. Batch rule
        BatchRule rule = BatchRule.builder()
                .createdAt(LocalDateTime.now())
                .condition(condition)
                .reward(reward)
                .build();

        rule = batchRuleRepository.saveAndFlush(rule);

        // 6. Voucher batch
        VoucherBatch batch = VoucherBatch.builder()
                .createdAt(LocalDateTime.now())
                .campaign(campaign)
                .rule(rule)
                .batchType(batchType)
                .codeConfigJson(
                        objectMapper.readTree("{}")
                )
                .distributionChannelsJson(
                        objectMapper.readTree("[]")
                )
                .durationUnit(DurationUnit.DAYS)
                .durationValue(7)
                .generatedCount(generatedCount)
                .maxUsageCount(maxUsageCount)
                .status(BatchStatus.GENERATED)
                .build();

        batch = voucherBatchRepository.saveAndFlush(batch);

        // 7. Voucher
        String code = "IT-" + UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 12)
                .toUpperCase();

        Voucher voucher = Voucher.builder()
                .createdAt(LocalDateTime.now())
                .batch(batch)
                .campaign(campaign)
                .code(code)
                .build();

        voucher = voucherRepository.saveAndFlush(voucher);

        return new Fixture(
                voucher.getId(),
                batch.getId(),
                campaign.getId(),
                code
        );
    }
}
