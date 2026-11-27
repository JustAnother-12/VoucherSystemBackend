package com.dacn.VoucherSystem.seeder;

import com.dacn.VoucherSystem.entity.*;
import com.dacn.VoucherSystem.enums.*;
import com.dacn.VoucherSystem.repository.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * One-time, non-destructive demo data initializer for the production profile.
 * The existing DatabaseSeeder remains dedicated to the dev profile.
 */
@Slf4j
@Component
@Profile("prod")
@Order(Ordered.HIGHEST_PRECEDENCE)
@ConditionalOnProperty(
        prefix = "app.demo-seed",
        name = "enabled",
        havingValue = "true"
)
@RequiredArgsConstructor
public class DemoDataSeeder implements ApplicationRunner {

    private static final String SEED_KEY = "voucher-demo-v1";
    private static final long ADVISORY_LOCK_ID = 731_202_611_27L;

    private final AdminAccountRepository adminAccountRepository;
    private final CampaignRepository campaignRepository;
    private final ConditionRepository conditionRepository;
    private final RewardRepository rewardRepository;
    private final BatchRuleRepository batchRuleRepository;
    private final VoucherBatchRepository voucherBatchRepository;
    private final VoucherRepository voucherRepository;
    private final DistributionLogRepository distributionLogRepository;
    private final RedemptionLogRepository redemptionLogRepository;
    private final FraudLogRepository fraudLogRepository;
    private final CustomerVoucherRepository customerVoucherRepository;
    private final RateLimitConfigRepository rateLimitConfigRepository;
    private final BlackListRepository blackListRepository;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    @PersistenceContext
    private EntityManager entityManager;

    @Value("${app.demo-seed.admin-username:demo_admin}")
    private String adminUsername;

    @Value("${app.demo-seed.admin-password:}")
    private String adminPassword;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        // Serialize the first run if Railway starts more than one API instance.
        entityManager.createNativeQuery("SELECT pg_advisory_xact_lock(" + ADVISORY_LOCK_ID + ")")
                .getSingleResult();

        if (seedMarkerExists()) {
            log.info("Demo seed '{}' has already been applied; skipping", SEED_KEY);
            return;
        }

        if (!isDemoDatabaseEmpty()) {
            log.warn("Demo seed skipped: application tables are not empty. No production data was changed.");
            return;
        }

        log.info("Starting one-time demo data seeding: {}", SEED_KEY);
        seedDemoData();
        syncSeededRedemptionCounters();

        entityManager.createNativeQuery("""
                INSERT INTO demo_seed_history (seed_key, seeded_at)
                VALUES (:seedKey, CURRENT_TIMESTAMP)
                """)
                .setParameter("seedKey", SEED_KEY)
                .executeUpdate();

        entityManager.flush();
        log.info("Demo data seeding completed successfully: {}", SEED_KEY);
    }

    private boolean seedMarkerExists() {
        return (Boolean) entityManager.createNativeQuery("""
                SELECT EXISTS (
                    SELECT 1 FROM demo_seed_history WHERE seed_key = :seedKey
                )
                """)
                .setParameter("seedKey", SEED_KEY)
                .getSingleResult();
    }

    /**
     * Intentionally checks every table populated by this demo seeder.
     * The seeder never truncates or deletes existing production records.
     */
    private boolean isDemoDatabaseEmpty() {
        Number rowCount = (Number) entityManager.createNativeQuery("""
                SELECT
                    (SELECT COUNT(*) FROM campaigns) +
                    (SELECT COUNT(*) FROM conditions) +
                    (SELECT COUNT(*) FROM rewards) +
                    (SELECT COUNT(*) FROM batch_rules) +
                    (SELECT COUNT(*) FROM voucher_batches) +
                    (SELECT COUNT(*) FROM vouchers) +
                    (SELECT COUNT(*) FROM distribution_logs) +
                    (SELECT COUNT(*) FROM redemption_logs) +
                    (SELECT COUNT(*) FROM fraud_logs) +
                    (SELECT COUNT(*) FROM customer_vouchers) +
                    (SELECT COUNT(*) FROM rate_limit_configs) +
                    (SELECT COUNT(*) FROM black_lists) +
                    (SELECT COUNT(*) FROM voucher_batch_redemption_counters)
                """)
                .getSingleResult();
        return rowCount.longValue() == 0L;
    }

    private void seedDemoData() {
        LocalDateTime now = LocalDateTime.now(clock).withNano(0);
        AdminAccount admin = createDemoAdmin(now);

        List<Campaign> campaigns = createCampaigns(admin, now);
        List<Condition> conditions = createConditions(now);
        List<Reward> rewards = createRewards(now);
        List<BatchRule> rules = createBatchRules(rewards, conditions, now);
        List<VoucherBatch> batches = createVoucherBatches(campaigns, rules, now);
        List<Voucher> vouchers = createVouchers(batches, campaigns, now);

        createDistributionLogs(vouchers, campaigns, now);
        createRedemptionLogs(vouchers, now);
        createCustomerVouchers(vouchers, now);
        createFraudLogs(vouchers, now);
        createRateLimitConfigs(now);
        createBlackLists(now);
    }

    private AdminAccount createDemoAdmin(LocalDateTime now) {
        var existing = adminAccountRepository.findByUsername(adminUsername);
        if (existing.isPresent()) {
            AdminAccount account = existing.get();
            if (!Boolean.TRUE.equals(account.getIsActive())) {
                throw new IllegalStateException(
                        "The configured demo admin account exists but is inactive: " + adminUsername
                );
            }
            return account;
        }

        if (adminPassword == null || adminPassword.isBlank()) {
            throw new IllegalStateException(
                    "APP_DEMO_SEED_ADMIN_PASSWORD is required to create the demo admin account"
            );
        }

        AdminAccount admin = AdminAccount.builder()
                .username(adminUsername)
                .hashedPassword(passwordEncoder.encode(adminPassword))
                .isActive(true)
                .role(AdminRole.SYSTEM)
                .createdAt(now)
                .build();
        return adminAccountRepository.saveAndFlush(admin);
    }

    private List<Campaign> createCampaigns(AdminAccount admin, LocalDateTime now) {
        List<Campaign> campaigns = List.of(
                campaign("Demo Welcome Campaign", now.minusDays(2), now.plusDays(30), CampaignStatus.ACTIVE, admin, now),
                campaign("VIP Member Rewards", now.minusDays(1), now.plusDays(21), CampaignStatus.ACTIVE, admin, now),
                campaign("Upcoming Flash Sale", now.plusDays(2), now.plusDays(5), CampaignStatus.DRAFT, admin, now),
                campaign("Tiered Discount Demo", now.minusDays(1), now.plusDays(14), CampaignStatus.ACTIVE, admin, now)
        );
        return campaignRepository.saveAllAndFlush(campaigns);
    }

    private Campaign campaign(
            String name,
            LocalDateTime start,
            LocalDateTime end,
            CampaignStatus status,
            AdminAccount admin,
            LocalDateTime now
    ) {
        return Campaign.builder()
                .campaignName(name)
                .startTime(start)
                .endTime(end)
                .status(status)
                .admin(admin)
                .createdAt(now)
                .build();
    }

    private List<Condition> createConditions(LocalDateTime now) {
        List<Condition> conditions = List.of(
                Condition.builder()
                        .conditionName("Đơn hàng tối thiểu 200.000đ")
                        .conditionType(ConditionType.MIN_CART_AMOUNT)
                        .conditionJson(json(Map.of(
                                "minimumAmount", 200000,
                                "amountBasis", "CART_SUBTOTAL"
                        )))
                        .status(RuleStatus.PUBLISHED)
                        .createdAt(now)
                        .build(),
                Condition.builder()
                        .conditionName("Chỉ áp dụng cho khách hàng VIP")
                        .conditionType(ConditionType.CUSTOMER_SEGMENT)
                        .conditionJson(json(Map.of(
                                "operator", "IN",
                                "segments", List.of("VIP")
                        )))
                        .status(RuleStatus.PUBLISHED)
                        .createdAt(now)
                        .build(),
                Condition.builder()
                        .conditionName("Khách VIP, đơn từ 500.000đ")
                        .conditionType(ConditionType.COMPOSITE)
                        .conditionJson(json(Map.of(
                                "operator", "AND",
                                "conditions", List.of(
                                        Map.of("type", "CUSTOMER_SEGMENT", "config", Map.of(
                                                "operator", "IN", "segments", List.of("VIP")
                                        )),
                                        Map.of("type", "MIN_CART_AMOUNT", "config", Map.of(
                                                "minimumAmount", 500000, "amountBasis", "CART_SUBTOTAL"
                                        ))
                                )
                        )))
                        .status(RuleStatus.PUBLISHED)
                        .createdAt(now)
                        .build(),
                Condition.builder()
                        .conditionName("Khung giờ áp dụng cả ngày")
                        .conditionType(ConditionType.TIME_WINDOW)
                        .conditionJson(json(Map.of(
                                "timezone", "Asia/Ho_Chi_Minh",
                                "windows", List.of(Map.of(
                                        "startTime", "00:00:00",
                                        "endTime", "23:59:59"
                                ))
                        )))
                        .status(RuleStatus.PUBLISHED)
                        .createdAt(now)
                        .build()
        );
        return conditionRepository.saveAllAndFlush(conditions);
    }

    private List<Reward> createRewards(LocalDateTime now) {
        List<Reward> rewards = List.of(
                Reward.builder()
                        .rewardName("Giảm 10% tối đa 50.000đ")
                        .rewardType(RewardType.PERCENTAGE)
                        .rewardJson(json(Map.of(
                                "discountPercent", 10,
                                "maxDiscountAmount", 50000,
                                "amountBasis", "CART_SUBTOTAL"
                        )))
                        .status(RuleStatus.PUBLISHED)
                        .createdAt(now)
                        .build(),
                Reward.builder()
                        .rewardName("Giảm trực tiếp 30.000đ")
                        .rewardType(RewardType.FIXED)
                        .rewardJson(json(Map.of("discountValue", 30000)))
                        .status(RuleStatus.PUBLISHED)
                        .createdAt(now)
                        .build(),
                Reward.builder()
                        .rewardName("Mua sổ tay tặng bút")
                        .rewardType(RewardType.BUY_X_GET_Y)
                        .rewardJson(json(Map.of(
                                "calculationMode", "PER_QUALIFYING_SET",
                                "buy", Map.of("productIds", List.of("DEMO-NOTEBOOK"), "quantity", 1),
                                "get", Map.of("productIds", List.of("DEMO-PEN"), "quantity", 1, "discountPercent", 100),
                                "maxApplicationsPerOrder", 1
                        )))
                        .status(RuleStatus.PUBLISHED)
                        .createdAt(now)
                        .build(),
                Reward.builder()
                        .rewardName("Giảm theo bậc 5% đến 15%")
                        .rewardType(RewardType.TIERED_PERCENTAGE)
                        .rewardJson(json(Map.of(
                                "calculationMode", "HIGHEST_MATCH",
                                "amountBasis", "CART_SUBTOTAL",
                                "tiers", List.of(
                                        Map.of("minCartAmount", 200000, "discountPercent", 5, "maxDiscountAmount", 30000),
                                        Map.of("minCartAmount", 500000, "discountPercent", 10, "maxDiscountAmount", 80000),
                                        Map.of("minCartAmount", 1000000, "discountPercent", 15, "maxDiscountAmount", 150000)
                                )
                        )))
                        .status(RuleStatus.PUBLISHED)
                        .createdAt(now)
                        .build()
        );
        return rewardRepository.saveAllAndFlush(rewards);
    }

    private List<BatchRule> createBatchRules(
            List<Reward> rewards,
            List<Condition> conditions,
            LocalDateTime now
    ) {
        List<BatchRule> rules = List.of(
                rule(rewards.get(0), conditions.get(0), now),
                rule(rewards.get(1), conditions.get(1), now),
                rule(rewards.get(2), conditions.get(2), now),
                rule(rewards.get(3), conditions.get(3), now)
        );
        return batchRuleRepository.saveAllAndFlush(rules);
    }

    private BatchRule rule(Reward reward, Condition condition, LocalDateTime now) {
        return BatchRule.builder()
                .reward(reward)
                .condition(condition)
                .createdAt(now)
                .build();
    }

    private List<VoucherBatch> createVoucherBatches(
            List<Campaign> campaigns,
            List<BatchRule> rules,
            LocalDateTime now
    ) {
        List<VoucherBatch> batches = List.of(
                batch(campaigns.get(0), rules.get(0), BatchType.GENERIC,
                        "WELCOME", 30, DurationUnit.DAYS, 3, 10, BatchStatus.GENERATED, now),
                batch(campaigns.get(1), rules.get(1), BatchType.BULK,
                        "VIP", 60, DurationUnit.DAYS, 2, 5, BatchStatus.GENERATED, now),
                batch(campaigns.get(2), rules.get(2), BatchType.BULK,
                        "FLASH", 7, DurationUnit.DAYS, 0, 10, BatchStatus.DRAFT, now),
                batch(campaigns.get(3), rules.get(3), BatchType.GENERIC,
                        "TIER", 14, DurationUnit.DAYS, 1, 5, BatchStatus.GENERATED, now)
        );
        // V9 creates redemption-counter shards automatically on each batch insert.
        return voucherBatchRepository.saveAllAndFlush(batches);
    }

    private VoucherBatch batch(
            Campaign campaign,
            BatchRule rule,
            BatchType type,
            String prefix,
            int durationValue,
            DurationUnit durationUnit,
            int generatedCount,
            int maxUsageCount,
            BatchStatus status,
            LocalDateTime now
    ) {
        return VoucherBatch.builder()
                .campaign(campaign)
                .rule(rule)
                .batchType(type)
                .codeConfigJson(json(Map.of(
                        "prefix", prefix,
                        "length", 8,
                        "charset", "ALPHANUMERIC"
                )))
                .durationUnit(durationUnit)
                .durationValue(durationValue)
                .generatedCount(generatedCount)
                .maxUsageCount(maxUsageCount)
                .distributionChannelsJson(json(Map.of(
                        "allocations", List.of(
                                Map.of("channel", "EMAIL", "quota", 10),
                                Map.of("channel", "WEBHOOK", "quota", 5)
                        )
                )))
                .status(status)
                .createdAt(now)
                .build();
    }

    private List<Voucher> createVouchers(
            List<VoucherBatch> batches,
            List<Campaign> campaigns,
            LocalDateTime now
    ) {
        List<Voucher> vouchers = List.of(
                voucher(batches.get(0), campaigns.get(0), "DEMO-WELCOME-001", now),
                voucher(batches.get(0), campaigns.get(0), "DEMO-WELCOME-002", now),
                voucher(batches.get(0), campaigns.get(0), "DEMO-WELCOME-003", now),
                voucher(batches.get(1), campaigns.get(1), "DEMO-VIP-001", now),
                voucher(batches.get(1), campaigns.get(1), "DEMO-VIP-002", now),
                voucher(batches.get(3), campaigns.get(3), "DEMO-TIERED-001", now)
        );
        return voucherRepository.saveAllAndFlush(vouchers);
    }

    private Voucher voucher(VoucherBatch batch, Campaign campaign, String code, LocalDateTime now) {
        return Voucher.builder()
                .batch(batch)
                .campaign(campaign)
                .code(code)
                .createdAt(now)
                .build();
    }

    private void createDistributionLogs(
            List<Voucher> vouchers,
            List<Campaign> campaigns,
            LocalDateTime now
    ) {
        distributionLogRepository.saveAll(List.of(
                distribution(vouchers.get(0), campaigns.get(0), "EMAIL", "demo.customer01@example.test", DistributionStatus.SENT, now.minusHours(4)),
                distribution(vouchers.get(1), campaigns.get(0), "EMAIL", "demo.customer02@example.test", DistributionStatus.SENT, now.minusHours(3)),
                distribution(vouchers.get(3), campaigns.get(1), "WEBHOOK", "https://demo.example.test/vouchers/claim", DistributionStatus.SENT, now.minusHours(2)),
                distribution(vouchers.get(4), campaigns.get(1), "EMAIL", "demo.vip@example.test", DistributionStatus.PENDING, now.minusHours(1)),
                distribution(vouchers.get(5), campaigns.get(3), "EMAIL", "demo.tiered@example.test", DistributionStatus.SENT, now.minusMinutes(30))
        ));
    }

    private DistributionLog distribution(
            Voucher voucher,
            Campaign campaign,
            String channel,
            String target,
            DistributionStatus status,
            LocalDateTime sentAt
    ) {
        return DistributionLog.builder()
                .voucher(voucher)
                .campaign(campaign)
                .channel(channel)
                .targetAddress(target)
                .status(status)
                .sentAt(sentAt)
                .build();
    }

    private void createRedemptionLogs(List<Voucher> vouchers, LocalDateTime now) {
        redemptionLogRepository.saveAll(List.of(
                redemption(vouchers.get(0), "demo_customer_001", "192.0.2.10", "demo-device-001",
                        "500000.00", "50000.00", "450000.00", RedemptionStatus.SUCCESS, null, now.minusHours(3)),
                redemption(vouchers.get(1), "demo_customer_002", "192.0.2.11", "demo-device-002",
                        "300000.00", "30000.00", "270000.00", RedemptionStatus.SUCCESS, null, now.minusHours(2)),
                redemption(vouchers.get(3), "demo_customer_003", "192.0.2.12", "demo-device-003",
                        "800000.00", "30000.00", "770000.00", RedemptionStatus.SUCCESS, null, now.minusHours(1)),
                redemption(vouchers.get(2), "demo_customer_004", "192.0.2.13", "demo-device-004",
                        "100000.00", "0.00", "100000.00", RedemptionStatus.FAILED,
                        "Cart amount does not satisfy voucher condition", now.minusMinutes(30)),
                redemption(vouchers.get(5), "demo_customer_006", "192.0.2.14", "demo-device-006",
                        "1200000.00", "150000.00", "1050000.00", RedemptionStatus.SUCCESS,
                        null, now.minusMinutes(15))
        ));
    }

    private RedemptionLog redemption(
            Voucher voucher,
            String customerId,
            String ip,
            String device,
            String original,
            String discount,
            String total,
            RedemptionStatus status,
            String failureReason,
            LocalDateTime createdAt
    ) {
        return RedemptionLog.builder()
                .voucher(voucher)
                .customerId(customerId)
                .ipAddress(ip)
                .deviceFingerprint(device)
                .originalAmount(new BigDecimal(original))
                .discountAmount(new BigDecimal(discount))
                .finalAmount(new BigDecimal(total))
                .status(status)
                .failureReason(failureReason)
                .createdAt(createdAt)
                .build();
    }

    private void createCustomerVouchers(List<Voucher> vouchers, LocalDateTime now) {
        LocalDateTime claimedAt = now.minusHours(2);
        customerVoucherRepository.saveAll(List.of(
                customerVoucher(vouchers.get(0), "demo_customer_001", CustomerVoucherStatus.USED, claimedAt, claimedAt.plusDays(30)),
                customerVoucher(vouchers.get(1), "demo_customer_002", CustomerVoucherStatus.USED, claimedAt, claimedAt.plusDays(30)),
                customerVoucher(vouchers.get(2), "demo_customer_004", CustomerVoucherStatus.AVAILABLE, claimedAt, claimedAt.plusDays(30)),
                customerVoucher(vouchers.get(3), "demo_customer_003", CustomerVoucherStatus.USED, claimedAt, claimedAt.plusDays(60)),
                customerVoucher(vouchers.get(4), "demo_customer_005", CustomerVoucherStatus.AVAILABLE, claimedAt, claimedAt.plusDays(60)),
                customerVoucher(vouchers.get(5), "demo_customer_006", CustomerVoucherStatus.USED, claimedAt, claimedAt.plusDays(14))
        ));
    }

    private CustomerVoucher customerVoucher(
            Voucher voucher,
            String customerId,
            CustomerVoucherStatus status,
            LocalDateTime claimedAt,
            LocalDateTime expiredAt
    ) {
        return CustomerVoucher.builder()
                .voucher(voucher)
                .customerId(customerId)
                .status(status)
                .claimedAt(claimedAt)
                .expiredAt(expiredAt)
                .build();
    }

    private void createFraudLogs(List<Voucher> vouchers, LocalDateTime now) {
        fraudLogRepository.saveAll(List.of(
                fraud(vouchers.get(0), "demo_flagged_customer_001", "203.0.113.21", "demo-suspicious-device-01",
                        "EXCEEDED_RATE_LIMIT", now.minusMinutes(50)),
                fraud(vouchers.get(3), "demo_flagged_customer_002", "203.0.113.22", "demo-suspicious-device-02",
                        "SAME_DEVICE_MULTIPLE_ACCOUNTS", now.minusMinutes(25)),
                fraud(null, "demo_flagged_customer_003", "203.0.113.23", "demo-suspicious-device-03",
                        "SUSPICIOUS_REQUEST_PATTERN", now.minusMinutes(10))
        ));
    }

    private FraudLog fraud(
            Voucher voucher,
            String customerId,
            String ip,
            String device,
            String rule,
            LocalDateTime createdAt
    ) {
        return FraudLog.builder()
                .voucher(voucher)
                .customerId(customerId)
                .ipAddress(ip)
                .deviceFingerprint(device)
                .violationRule(rule)
                .createdAt(createdAt)
                .build();
    }

    private void createRateLimitConfigs(LocalDateTime now) {
        rateLimitConfigRepository.saveAll(List.of(
                rateLimit("Demo redemption limit by customer", "/redemptions", LimitScope.CUSTOMER_ID,
                        10, 60, ActionOnExceed.REJECT_429, 1, now),
                rateLimit("Demo abuse prevention by device", "/redemptions", LimitScope.DEVICE_FINGERPRINT,
                        5, 300, ActionOnExceed.BLACKLIST, 2, now),
                rateLimit("Demo general API limit", "/**", LimitScope.IP,
                        120, 60, ActionOnExceed.REJECT_429, 10, now)
        ));
    }

    private RateLimitConfig rateLimit(
            String name,
            String endpoint,
            LimitScope scope,
            int maxRequest,
            int windowSeconds,
            ActionOnExceed action,
            int priority,
            LocalDateTime now
    ) {
        return RateLimitConfig.builder()
                .rateLimitConfigName(name)
                .endpointPattern(endpoint)
                .limitScope(scope)
                .maxRequest(maxRequest)
                .timeWindowSeconds(windowSeconds)
                .actionOnExceed(action)
                .priority(priority)
                .isActive(true)
                .createdAt(now)
                .build();
    }

    private void createBlackLists(LocalDateTime now) {
        blackListRepository.saveAll(List.of(
                blackList(BlacklistType.CUSTOMER, "demo_blocked_customer", 
                        "Demo record: customer is blocked for fraud testing", true, now.plusDays(30), now),
                blackList(BlacklistType.DEVICE_FINGERPRINT, "demo_blocked_device",
                        "Demo record: device is blocked for fraud testing", true, now.plusDays(30), now),
                blackList(BlacklistType.IP, "203.0.113.99",
                        "Demo record: inactive IP blacklist example", false, now.minusDays(1), now)
        ));
    }

    private BlackList blackList(
            BlacklistType type,
            String target,
            String reason,
            boolean active,
            LocalDateTime expiredAt,
            LocalDateTime createdAt
    ) {
        return BlackList.builder()
                .blacklistType(type)
                .targetValue(target)
                .reason(reason)
                .isActive(active)
                .expiredAt(expiredAt)
                .createdAt(createdAt)
                .build();
    }

    private JsonNode json(Map<String, ?> values) {
        return objectMapper.valueToTree(values);
    }

    /** Keep the sharded counters (V9) consistent with seeded successful redemptions. */
    private void syncSeededRedemptionCounters() {
        Number invalidBatchCount = (Number) entityManager.createNativeQuery("""
                SELECT COUNT(*)
                FROM (
                    SELECT b.id, b.max_usage_count
                    FROM voucher_batches b
                    LEFT JOIN vouchers v ON v.batch_id = b.id
                    LEFT JOIN redemption_logs rl
                        ON rl.voucher_id = v.id AND rl.status = 'SUCCESS'
                    GROUP BY b.id, b.max_usage_count
                    HAVING COUNT(rl.id) > b.max_usage_count
                ) invalid_batches
                """).getSingleResult();

        if (invalidBatchCount.longValue() > 0) {
            throw new IllegalStateException("Seeded successful redemptions exceed batch quota");
        }

        entityManager.createNativeQuery("""
                WITH batch_usage AS (
                    SELECT b.id AS batch_id, COUNT(rl.id)::INT AS total_redeemed
                    FROM voucher_batches b
                    LEFT JOIN vouchers v ON v.batch_id = b.id
                    LEFT JOIN redemption_logs rl
                        ON rl.voucher_id = v.id AND rl.status = 'SUCCESS'
                    GROUP BY b.id
                ),
                allocated_shards AS (
                    SELECT c.batch_id, c.shard_id, c.shard_limit, u.total_redeemed,
                        COALESCE(
                            SUM(c.shard_limit) OVER (
                                PARTITION BY c.batch_id
                                ORDER BY c.shard_id
                                ROWS BETWEEN UNBOUNDED PRECEDING AND 1 PRECEDING
                            ), 0
                        ) AS capacity_before
                    FROM voucher_batch_redemption_counters c
                    JOIN batch_usage u ON u.batch_id = c.batch_id
                )
                UPDATE voucher_batch_redemption_counters c
                SET redeemed_count = LEAST(
                    s.shard_limit,
                    GREATEST(0, s.total_redeemed - s.capacity_before)
                )
                FROM allocated_shards s
                WHERE c.batch_id = s.batch_id AND c.shard_id = s.shard_id
                """).executeUpdate();
    }
}
