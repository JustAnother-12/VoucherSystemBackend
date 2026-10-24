package com.dacn.VoucherSystem.config;

import com.dacn.VoucherSystem.entity.*;
import com.dacn.VoucherSystem.enums.*;
import com.dacn.VoucherSystem.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Component
@Profile("dev") // Only run in dev env
@RequiredArgsConstructor
public class DatabaseSeeder implements CommandLineRunner {
//    private final UserRepository userRepository;
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
    public void run(String... args) throws Exception {
        System.out.println("Currently seeding Database...");

        if (adminAccountRepository.count() == 0) {
            // 1. Seed Admin Accounts (4 bản ghi)
            AdminAccount admin1 = AdminAccount.builder()
                    .username("superadmin")
                    .hashedPassword("SuperAdmin123")
                    .isActive(true)
                    .role(AdminRole.SYSTEM)
                    .build();

            AdminAccount admin2 = AdminAccount.builder()
                    .username("marketing_lead")
                    .hashedPassword("MarketLead123")
                    .isActive(true)
                    .role(AdminRole.CAMPAIN_MANAGER)
                    .build();

            AdminAccount admin3 = AdminAccount.builder()
                    .username("security_lead")
                    .hashedPassword("SecurityLead123")
                    .isActive(true)
                    .role(AdminRole.SECURITY_MANAGER)
                    .build();

            AdminAccount admin4 = AdminAccount.builder()
                    .username("finance_lead")
                    .hashedPassword("FinanceLead123")
                    .isActive(true)
                    .role(AdminRole.FINANCE_MANAGER)
                    .build();

            adminAccountRepository.saveAll(List.of(admin1, admin2, admin3, admin4));

            // 2. Seed Campaigns (4 bản ghi)
            Campaign camp1 = Campaign.builder()
                    .name("Flash Sale Hè 2026")
                    .startTime(LocalDateTime.now().minusDays(2))
                    .endTime(LocalDateTime.now().plusDays(5))
                    .status(CampainStatus.ACTIVE)
                    .admin(admin2)
                    .build();

            Campaign camp2 = Campaign.builder()
                    .name("Chào mừng Khách hàng mới")
                    .startTime(LocalDateTime.now().minusDays(10))
                    .endTime(LocalDateTime.now().plusDays(20))
                    .status(CampainStatus.ACTIVE)
                    .admin(admin2)
                    .build();

            Campaign camp3 = Campaign.builder()
                    .name("Siêu Sale Black Friday (Dự thảo)")
                    .startTime(LocalDateTime.now().plusDays(30))
                    .endTime(LocalDateTime.now().plusDays(35))
                    .status(CampainStatus.DRAFT)
                    .admin(admin2)
                    .build();

            Campaign camp4 = Campaign.builder()
                    .name("Tri ân Khách hàng VIP năm cũ")
                    .startTime(LocalDateTime.now().minusDays(30))
                    .endTime(LocalDateTime.now().minusDays(1))
                    .status(CampainStatus.ENDED)
                    .admin(admin2)
                    .build();

            campaignRepository.saveAll(List.of(camp1, camp2, camp3, camp4));

            // 3. Seed Rewards (2 bản ghi)
            Reward reward1 = Reward.builder()
                    .name("Giảm 50K đơn từ 200K")
                    .rewardJson(readJsonFile("data/rewards/reward_fixed.json"))
                    .build();

            Reward reward2 = Reward.builder()
                    .name("Giảm 20% tối đa 100K")
                    .rewardJson(readJsonFile("data/rewards/reward_percent.json"))
                    .build();

            rewardRepository.saveAll(List.of(reward1, reward2));

            // 4. Seed Conditions (2 bản ghi)
            Condition cond1 = Condition.builder()
                    .name("Đơn hàng tối thiểu 200K")
                    .conditionJson(readJsonFile("data/conditions/min_cart_amount.json"))
                    .build();

            Condition cond2 = Condition.builder()
                    .name("Khung giờ vàng")
                    .conditionJson(readJsonFile("data/conditions/time_frame.json"))
                    .build();

            conditionRepository.saveAll(List.of(cond1, cond2));

            // 5. Seed Batch Rules (2 bản ghi)
            BatchRule rule1 = BatchRule.builder()
                    .reward(reward1)
                    .condition(cond1)
                    .build();

            BatchRule rule2 = BatchRule.builder()
                    .reward(reward2)
                    .condition(cond2)
                    .build();

            batchRuleRepository.saveAll(List.of(rule1, rule2));

            // 6. Seed Voucher Batches (Phân biệt GENERIC và BULK)
            // Batch 1: GENERIC (Chỉ dùng chung 1 mã code duy nhất cho toàn bộ batch)
            VoucherBatch batch1 = VoucherBatch.builder()
                    .campaign(camp1)
                    .rule(rule1)
                    .batchType(BatchType.GENERIC)
                    .codeConfigJson("{\"shared_code\": \"SUMMER2026\"}")
                    .durationValue(7)
                    .durationUnit("DAYS")
                    .generatedCount(10000)
                    .redeemedCount(15)
                    .distributionChannelsJson(readJsonFile("data/distributions/example_distribution.json"))
                    .build();

            // Batch 2: BULK (Mỗi voucher một mã code riêng biệt, sinh nhiều dòng trong bảng vouchers)
            VoucherBatch batch2 = VoucherBatch.builder()
                    .campaign(camp2)
                    .rule(rule2)
                    .batchType(BatchType.BULK)
                    .codeConfigJson("{\"prefix\": \"WELCOME\", \"length\": 6}")
                    .durationValue(30)
                    .durationUnit("DAYS")
                    .generatedCount(4)
                    .redeemedCount(2)
                    .distributionChannelsJson(readJsonFile("data/distributions/example_distribution.json"))
                    .build();

            voucherBatchRepository.saveAll(List.of(batch1, batch2));

            // Gán lại mối quan hệ 1-1 ngược cho BatchRule
            rule1.setVoucherBatch(batch1);
            rule2.setVoucherBatch(batch2);
            batchRuleRepository.saveAll(List.of(rule1, rule2));

            // 7. Seed Vouchers (Thể hiện đúng logic GENERIC vs BULK)
            // Đối với batch1 (GENERIC): Chỉ cần 1 dòng voucher đại diện cho mã chung "SUMMER2026"
            Voucher vGeneric1 = Voucher.builder()
                    .batch(batch1)
                    .campaign(camp1)
                    .code("SUMMER2026")
                    .build();

            // Đối với batch2 (BULK): Mỗi mã là một dòng riêng biệt nhưng chung batch_id
            Voucher vBulk1 = Voucher.builder()
                    .batch(batch2)
                    .campaign(camp2)
                    .code("BF-123")
                    .build();

            Voucher vBulk2 = Voucher.builder()
                    .batch(batch2)
                    .campaign(camp2)
                    .code("BF-234")
                    .build();

            Voucher vBulk3 = Voucher.builder()
                    .batch(batch2)
                    .campaign(camp2)
                    .code("BF-345")
                    .build();

            Voucher vBulk4 = Voucher.builder()
                    .batch(batch2)
                    .campaign(camp2)
                    .code("BF-456")
                    .build();

            voucherRepository.saveAll(List.of(vGeneric1, vBulk1, vBulk2, vBulk3, vBulk4));

            // 8. Seed Customer Vouchers
            CustomerVoucher cv1 = CustomerVoucher.builder()
                    .voucher(vGeneric1)
                    .customerId("CUST_001")
                    .status(CustomerVoucherStatus.AVAILABLE)
                    .claimedAt(LocalDateTime.now().minusHours(5))
                    .expiredAt(LocalDateTime.now().plusDays(2))
                    .build();

            CustomerVoucher cv2 = CustomerVoucher.builder()
                    .voucher(vBulk1)
                    .customerId("CUST_002")
                    .status(CustomerVoucherStatus.USED)
                    .claimedAt(LocalDateTime.now().minusDays(2))
                    .expiredAt(LocalDateTime.now().plusDays(28))
                    .build();

            CustomerVoucher cv3 = CustomerVoucher.builder()
                    .voucher(vBulk2)
                    .customerId("CUST_003")
                    .status(CustomerVoucherStatus.UNAVAILABLE)
                    .claimedAt(LocalDateTime.now().minusDays(1))
                    .expiredAt(LocalDateTime.now().plusDays(29))
                    .build();

            customerVoucherRepository.saveAll(List.of(cv1, cv2, cv3));

            // 9. Seed Redemption Logs
            RedemptionLog log1 = RedemptionLog.builder()
                    .voucher(vBulk1)
                    .customerId("CUST_002")
                    .ipAddress("192.168.1.50")
                    .deviceFingerprint("fp_device_abc123")
                    .originalAmount(new BigDecimal("250000.00"))
                    .discountAmount(new BigDecimal("50000.00"))
                    .finalAmount(new BigDecimal("200000.00"))
                    .status(RedemptionStatus.SUCCESS)
                    .createdAt(LocalDateTime.now().minusDays(1))
                    .build();

            RedemptionLog log2 = RedemptionLog.builder()
                    .voucher(vGeneric1)
                    .customerId("CUST_099")
                    .ipAddress("10.0.0.15")
                    .deviceFingerprint("fp_device_xyz999")
                    .originalAmount(new BigDecimal("150000.00"))
                    .discountAmount(BigDecimal.ZERO)
                    .finalAmount(new BigDecimal("150000.00"))
                    .status(RedemptionStatus.FAILED)
                    .failureReason("CONDITION_NOT_MET")
                    .createdAt(LocalDateTime.now().minusHours(10))
                    .build();

            redemptionLogRepository.saveAll(List.of(log1, log2));

            // 10. Seed Fraud Logs
            FraudLog fLog1 = FraudLog.builder()
                    .voucher(vGeneric1)
                    .customerId("CUST_999")
                    .ipAddress("203.113.5.10")
                    .deviceFingerprint("fp_suspicious_001")
                    .violationRule("EXCESSIVE_REQUESTS_IP")
                    .createdAt(LocalDateTime.now().minusHours(3))
                    .build();

            FraudLog fLog2 = FraudLog.builder()
                    .voucher(vBulk1)
                    .customerId("CUST_888")
                    .ipAddress("14.161.22.5")
                    .deviceFingerprint("fp_suspicious_002")
                    .violationRule("MULTIPLE_ACCOUNTS_SAME_DEVICE")
                    .createdAt(LocalDateTime.now().minusDays(2))
                    .build();

            fraudLogRepository.saveAll(List.of(fLog1, fLog2));

            // 11. Seed Distribution Logs (Với DistributionStatus mới)
            DistributionLog dLog1 = DistributionLog.builder()
                    .campaign(camp1)
                    .voucher(vGeneric1)
                    .channel("SMS")
                    .targetAddress("+84901234567")
                    .status(DistributionStatus.SENT)
                    .sentAt(LocalDateTime.now().minusDays(2))
                    .build();

            DistributionLog dLog2 = DistributionLog.builder()
                    .campaign(camp2)
                    .voucher(vBulk1)
                    .channel("EMAIL")
                    .targetAddress("customer002@gmail.com")
                    .status(DistributionStatus.PENDING)
                    .sentAt(LocalDateTime.now().minusDays(5))
                    .build();

            DistributionLog dLog3 = DistributionLog.builder()
                    .campaign(camp2)
                    .voucher(vBulk2)
                    .channel("EMAIL")
                    .targetAddress("customer099@gmail.com")
                    .status(DistributionStatus.FAILED)
                    .sentAt(LocalDateTime.now().minusDays(4))
                    .build();

            distributionLogRepository.saveAll(List.of(dLog1, dLog2, dLog3));

//            // 12. Seed Rate Limit Configs
//            RateLimitConfig rlc1 = RateLimitConfig.builder()
//                    .name("Giới hạn API đổi voucher")
//                    .endpointPattern("/api/vouchers/redeem")
//                    .limitScope("IP")
//                    .maxRequest(5)
//                    .timeWindowSeconds(60)
//                    .actionOnExceed("BLOCK_TEMPORARY")
//                    .priority(1)
//                    .isActive(true)
//                    .createdAt(LocalDateTime.now())
//                    .build();
//
//            RateLimitConfig rlc2 = RateLimitConfig.builder()
//                    .name("Giới hạn yêu cầu mã OTP nhận voucher")
//                    .endpointPattern("/api/vouchers/claim")
//                    .limitScope("CUSTOMER")
//                    .maxRequest(3)
//                    .timeWindowSeconds(300)
//                    .actionOnExceed("THROTTLE")
//                    .priority(2)
//                    .isActive(true)
//                    .createdAt(LocalDateTime.now())
//                    .build();
//
//            rateLimitConfigRepository.saveAll(List.of(rlc1, rlc2));

            // 13. Seed Black List
            BlackList bl1 = BlackList.builder()
                    .type("IP")
                    .value("203.113.5.10")
                    .reason("Phát hiện spam request / tấn công DDoS")
                    .isActive(true)
                    .expiredAt(LocalDateTime.now().plusDays(30))
                    .createdAt(LocalDateTime.now().minusDays(1))
                    .build();

            BlackList bl2 = BlackList.builder()
                    .type("DEVICE_FINGERPRINT")
                    .value("fp_suspicious_002")
                    .reason("Gian lận đa tài khoản trục lợi voucher")
                    .isActive(true)
                    .expiredAt(LocalDateTime.now().plusDays(90))
                    .createdAt(LocalDateTime.now().minusDays(3))
                    .build();

            blackListRepository.saveAll(List.of(bl1, bl2));
            System.out.println("Seeding finished!");
        }
    }
}
