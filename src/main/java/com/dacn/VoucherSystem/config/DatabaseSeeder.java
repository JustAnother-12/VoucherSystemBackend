package com.dacn.VoucherSystem.config;

import com.dacn.VoucherSystem.entity.*;
import com.dacn.VoucherSystem.enums.*;
import com.dacn.VoucherSystem.repository.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;

@Component
@Profile("dev")
@ConditionalOnProperty(
        prefix = "app.seed",
        name = "enabled",
        havingValue = "true"
)
@RequiredArgsConstructor
public class DatabaseSeeder implements CommandLineRunner {
    private final AdminAccountRepository adminAccountRepository;
    private final CampaignRepository campaignRepository;
    private final RewardRepository rewardRepository;
    private final ConditionRepository conditionRepository;
    private final BatchRuleRepository batchRuleRepository;
    private final VoucherBatchRepository voucherBatchRepository;
    private final VoucherRepository voucherRepository;
    private final CustomerVoucherRepository customerVoucherRepository;
    private final RedemptionLogRepository redemptionLogRepository;
    private final FraudLogRepository fraudLogRepository;
    private final DistributionLogRepository distributionLogRepository;
    private final RateLimitConfigRepository rateLimitConfigRepository;
    private final BlackListRepository blackListRepository;

    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    @PersistenceContext
    private EntityManager entityManager;

    private final Environment environment;

    @Value("${app.seed.allow-destructive:false}")
    private boolean allowDestructive;

    private String readJsonFile(String path) {
        ClassPathResource resource = new ClassPathResource(path);
        try(InputStream inputStream = resource.getInputStream()) {
            return new String(
                    inputStream.readAllBytes(),
                    java.nio.charset.StandardCharsets.UTF_8
            );
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Failed to load seed data from: " + path, e
            );
        }
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        System.out.println("Currently seeding Database...");
        verifyDestructiveSeedAllowed();

        ObjectMapper objectMapper = new ObjectMapper();

        resetDatabase();

        seedAdminAccounts(objectMapper);
        seedCampaigns(objectMapper);
        seedConditions(objectMapper);
        seedRewards(objectMapper);
        seedBatchRules(objectMapper);
        seedVoucherBatches(objectMapper);
        seedVouchers(objectMapper);
        seedDistributionLogs(objectMapper);
        seedRedemptionLogs(objectMapper);
        seedFraudLogs(objectMapper);
        seedCustomerVouchers(objectMapper);
        seedRateLimitConfigs(objectMapper);
        seedBlackLists(objectMapper);

        System.out.println("Database reset and seed completed.");
    }

    private void verifyDestructiveSeedAllowed() {
        if (!allowDestructive) {
            throw new IllegalStateException(
                    "Database seeding requires app.seed.allow-destructive=true"
            );
        }

        if (!environment.acceptsProfiles(Profiles.of("dev"))
                || environment.acceptsProfiles(Profiles.of("prod"))) {
            throw new IllegalStateException(
                    "Destructive seeding is allowed only in dev profile"
            );
        }
    }

    private void resetDatabase() {
        // clear the database except for migration history
        entityManager.flush();

        entityManager.createNativeQuery("""
        TRUNCATE TABLE
            sessions,
            redemption_logs,
            fraud_logs,
            distribution_logs,
            customer_vouchers,
            vouchers,
            voucher_batches,
            batch_rules,
            rewards,
            conditions,
            campaigns,
            rate_limit_configs,
            black_lists,
            admin_accounts
        RESTART IDENTITY CASCADE
        """).executeUpdate();

        entityManager.clear();
    }

    private void seedAdminAccounts(ObjectMapper objectMapper) {
        String jsonContent = readJsonFile("data/adminaccounts/admin_accounts.json");

        List<AdminAccount> adminAccounts = objectMapper.readValue(
                jsonContent,
                new TypeReference<List<AdminAccount>>() {}
        );

        for (AdminAccount admin : adminAccounts) {
            admin.setHashedPassword(
                    passwordEncoder.encode(admin.getHashedPassword())
            );
        }

        adminAccounts.forEach(e -> e.setCreatedAt(LocalDateTime.now(clock)));
        adminAccountRepository.saveAll(adminAccounts);
        System.out.println("Successfully seeded " + adminAccounts.size() + " admin accounts");
    }

    private void seedCampaigns(ObjectMapper objectMapper) {
        String jsonContent = readJsonFile("data/campaigns/campaigns.json");

        List<Campaign> campaigns = objectMapper.readValue(
                jsonContent,
                new TypeReference<List<Campaign>>() {}
        );
        for (Campaign campaign : campaigns) {
            Long adminId = campaign.getAdmin().getId();
            AdminAccount managedAdmin = adminAccountRepository.findById(adminId)
                    .orElseThrow(() -> new RuntimeException("Admin not found with ID: " + adminId));
            campaign.setAdmin(managedAdmin);
        }

        campaigns.forEach(e -> e.setCreatedAt(LocalDateTime.now(clock)));
        campaignRepository.saveAll(campaigns);
        System.out.println("Successfully seeded " + campaigns.size() + " campaigns");
    }

    private void seedConditions(ObjectMapper objectMapper) {
        String jsonContent = readJsonFile("data/conditions/conditions.json");

        JsonNode rootNode = objectMapper.readTree(jsonContent);
        List<Condition> conditions = new ArrayList<>();

        if (rootNode.isArray()) {
            for (JsonNode node : rootNode) {
                Condition condition = new Condition();

                condition.setConditionName(node.get("conditionName").asString());
                condition.setConditionType(ConditionType.valueOf(node.get("conditionType").asString()));
                condition.setStatus(RuleStatus.valueOf(node.get("status").asString()));

                JsonNode configNode = node.get("conditionJson");
                condition.setConditionJson(configNode);

                conditions.add(condition);
            }
        }

        conditions.forEach(e -> e.setCreatedAt(LocalDateTime.now(clock)));
        conditionRepository.saveAll(conditions);
        System.out.println("Successfully seeded " + conditions.size() + " conditions");
    }

    private void seedRewards(ObjectMapper objectMapper) {
        String jsonContent = readJsonFile("data/rewards/rewards.json");

        JsonNode rootNode = objectMapper.readTree(jsonContent);
        List<Reward> rewards = new ArrayList<>();

        if (rootNode.isArray()) {
            for (JsonNode node : rootNode) {
                Reward reward = new Reward();

                reward.setRewardName(node.get("rewardName").asString());
                reward.setRewardType(RewardType.valueOf(node.get("rewardType").asString()));
                reward.setStatus(RuleStatus.valueOf(node.get("status").asString()));

                JsonNode configNode = node.get("rewardJson");
                reward.setRewardJson(configNode);

                rewards.add(reward);
            }
        }

        rewards.forEach(e -> e.setCreatedAt(LocalDateTime.now(clock)));
        rewardRepository.saveAll(rewards);
        System.out.println("Successfully seeded " + rewards.size() + " rewards");
    }

    private void seedBatchRules(ObjectMapper objectMapper) {
        String jsonContent = readJsonFile("data/batchrules/batchrules.json");

        List<BatchRule> batchRules = objectMapper.readValue(
                jsonContent,
                new TypeReference<List<BatchRule>>() {}
        );
        for (BatchRule batchRule : batchRules) {
            Long rewardId = batchRule.getReward().getId();
            Reward reward = rewardRepository.findById(rewardId)
                    .orElseThrow(() -> new RuntimeException("Reward not found with ID: " + rewardId));
            batchRule.setReward(reward);

            Long conditionId = batchRule.getCondition().getId();
            Condition condition = conditionRepository.findById(conditionId)
                    .orElseThrow(() -> new RuntimeException("Condition not found with ID: " + conditionId));
            batchRule.setCondition(condition);
        }

        batchRules.forEach(e -> e.setCreatedAt(LocalDateTime.now(clock)));
        batchRuleRepository.saveAll(batchRules);
        System.out.println("Successfully seeded " + batchRules.size() + " batchRules");
    }

    private void seedVoucherBatches(ObjectMapper objectMapper) {
        String jsonContent = readJsonFile("data/voucherbatches/voucherbatches.json");

        JsonNode rootNode = objectMapper.readTree(jsonContent);
        List<VoucherBatch> voucherBatches = new ArrayList<>();

        if (rootNode.isArray()) {
            for (JsonNode node : rootNode) {
                VoucherBatch voucherBatch = new VoucherBatch();

                Long campaignId = node.get("campaign").get("id").asLong();
                Campaign campaign = campaignRepository.findById(campaignId)
                        .orElseThrow(() -> new RuntimeException("Campaign not found with ID: " + campaignId));
                voucherBatch.setCampaign(campaign);

                Long ruleId = node.get("rule").get("id").asLong();
                BatchRule batchRule = batchRuleRepository.findById(ruleId)
                        .orElseThrow(() -> new RuntimeException("BatchRule not found with ID: " + ruleId));
                voucherBatch.setRule(batchRule);

                voucherBatch.setBatchType(BatchType.valueOf(node.get("batchType").asString()));

                JsonNode codeConfigNode = node.get("codeConfigJson");
                voucherBatch.setCodeConfigJson(codeConfigNode);

                voucherBatch.setDurationUnit(DurationUnit.valueOf(node.get("durationUnit").asString()));

                voucherBatch.setDurationValue(node.get("durationValue").asInt());

                voucherBatch.setGeneratedCount(node.get("generatedCount").asInt());

                voucherBatch.setRedeemedCount(node.get("redeemedCount").asInt());

                JsonNode distributionNode = node.get("distributionChannelsJson");
                voucherBatch.setDistributionChannelsJson(distributionNode);

                voucherBatch.setStatus(BatchStatus.valueOf(node.get("status").asString()));

                voucherBatches.add(voucherBatch);
            }
        }

        voucherBatches.forEach(e -> e.setCreatedAt(LocalDateTime.now(clock)));
        voucherBatchRepository.saveAll(voucherBatches);
        System.out.println("Successfully seeded " + voucherBatches.size() + " voucherBatches");
    }

    private void seedVouchers(ObjectMapper objectMapper) {
        String jsonContent = readJsonFile("data/vouchers/vouchers.json");

        List<Voucher> vouchers = objectMapper.readValue(
                jsonContent,
                new TypeReference<List<Voucher>>() {}
        );
        for (Voucher voucher : vouchers) {
            Long batchId = voucher.getBatch().getId();
            VoucherBatch voucherBatch = voucherBatchRepository.findById(batchId)
                    .orElseThrow(() -> new RuntimeException("VoucherBatch not found with ID: " + batchId));
            voucher.setBatch(voucherBatch);

            Long campaignId = voucher.getCampaign().getId();
            Campaign campaign = campaignRepository.findById(campaignId)
                    .orElseThrow(() -> new RuntimeException("Campaign not found with ID: " + campaignId));
            voucher.setCampaign(campaign);
        }

        vouchers.forEach(e -> e.setCreatedAt(LocalDateTime.now(clock)));
        voucherRepository.saveAll(vouchers);
        System.out.println("Successfully seeded " + vouchers.size() + " vouchers");
    }

    private void seedDistributionLogs(ObjectMapper objectMapper) {
        String jsonContent = readJsonFile("data/distributionlogs/distributionlogs.json");

        List<DistributionLog> distributionLogs = objectMapper.readValue(
                jsonContent,
                new TypeReference<List<DistributionLog>>() {}
        );
        for (DistributionLog distributionLog : distributionLogs) {
            Long voucherId = distributionLog.getVoucher().getId();
            Voucher voucher = voucherRepository.findById(voucherId)
                    .orElseThrow(() -> new RuntimeException("Voucher not found with ID: " + voucherId));
            distributionLog.setVoucher(voucher);

            Long campaignId = distributionLog.getCampaign().getId();
            Campaign campaign = campaignRepository.findById(campaignId)
                    .orElseThrow(() -> new RuntimeException("Campaign not found with ID: " + campaignId));
            distributionLog.setCampaign(campaign);
        }

        distributionLogRepository.saveAll(distributionLogs);
        System.out.println("Successfully seeded " + distributionLogs.size() + " distributionLogs");
    }

    private void seedRedemptionLogs(ObjectMapper objectMapper) {
        String jsonContent = readJsonFile("data/redemptionlogs/redemptionlogs.json");

        List<RedemptionLog> redemptionLogs = objectMapper.readValue(
                jsonContent,
                new TypeReference<List<RedemptionLog>>() {}
        );
        for (RedemptionLog redemptionLog : redemptionLogs) {
            Long voucherId = redemptionLog.getVoucher().getId();
            Voucher voucher = voucherRepository.findById(voucherId)
                    .orElseThrow(() -> new RuntimeException("Voucher not found with ID: " + voucherId));
            redemptionLog.setVoucher(voucher);
        }

        redemptionLogs.forEach(e -> e.setCreatedAt(LocalDateTime.now(clock)));
        redemptionLogRepository.saveAll(redemptionLogs);
        System.out.println("Successfully seeded " + redemptionLogs.size() + " redemptionLogs");
    }

    private void seedFraudLogs(ObjectMapper objectMapper) {
        String jsonContent = readJsonFile("data/fraudlogs/fraudlogs.json");

        List<FraudLog> fraudLogs = objectMapper.readValue(
                jsonContent,
                new TypeReference<List<FraudLog>>() {}
        );
        for (FraudLog fraudLog : fraudLogs) {
            if (fraudLog.getVoucher() != null) {
                Long voucherId = fraudLog.getVoucher().getId();

                Voucher voucher = voucherRepository.findById(voucherId)
                        .orElseThrow(() -> new IllegalStateException(
                                "Voucher not found: " + voucherId
                        ));

                fraudLog.setVoucher(voucher);
            }
        }

        fraudLogs.forEach(e -> e.setCreatedAt(LocalDateTime.now(clock)));
        fraudLogRepository.saveAll(fraudLogs);
        System.out.println("Successfully seeded " + fraudLogs.size() + " fraudLogs");
    }

    private void seedCustomerVouchers(ObjectMapper objectMapper) {
        String jsonContent = readJsonFile("data/customervouchers/customervouchers.json");

        List<CustomerVoucher> customerVouchers = objectMapper.readValue(
                jsonContent,
                new TypeReference<List<CustomerVoucher>>() {}
        );
        for (CustomerVoucher customerVoucher : customerVouchers) {
            Long voucherId = customerVoucher.getVoucher().getId();
            Voucher voucher = voucherRepository.findById(voucherId)
                    .orElseThrow(() -> new RuntimeException("Voucher not found with ID: " + voucherId));
            customerVoucher.setVoucher(voucher);

            DurationUnit durationUnit = voucher.getBatch().getDurationUnit();
            Integer durationValue = voucher.getBatch().getDurationValue();

            LocalDateTime claimedAt = LocalDateTime.now(clock);
            customerVoucher.setClaimedAt(claimedAt);

            LocalDateTime expiredAt = switch (durationUnit) {
                case DAYS -> claimedAt.plusDays(durationValue);
                case HOURS -> claimedAt.plusHours(durationValue);
                case MINUTES -> claimedAt.plusMinutes(durationValue);
                case SECONDS -> claimedAt.plusSeconds(durationValue);
            };
            customerVoucher.setExpiredAt(expiredAt);
        }

        customerVoucherRepository.saveAll(customerVouchers);
        System.out.println("Successfully seeded " + customerVouchers.size() + " customerVouchers");
    }

    private void seedRateLimitConfigs(ObjectMapper objectMapper) {
        String jsonContent = readJsonFile("data/ratelimitconfigs/ratelimitconfigs.json");

        List<RateLimitConfig> rateLimitConfigs = objectMapper.readValue(
                jsonContent,
                new TypeReference<List<RateLimitConfig>>() {}
        );

        rateLimitConfigs.forEach(e -> e.setCreatedAt(LocalDateTime.now(clock)));
        rateLimitConfigRepository.saveAll(rateLimitConfigs);
        System.out.println("Successfully seeded " + rateLimitConfigs.size() + " rateLimitConfigs");
    }

    private void seedBlackLists(ObjectMapper objectMapper) {
        String jsonContent = readJsonFile("data/blacklists/blacklists.json");

        List<BlackList> blackLists = objectMapper.readValue(
                jsonContent,
                new TypeReference<List<BlackList>>() {}
        );

        blackLists.forEach(e -> e.setCreatedAt(LocalDateTime.now(clock)));
        blackListRepository.saveAll(blackLists);
        System.out.println("Successfully seeded " + blackLists.size() + " blackLists");
    }
}
