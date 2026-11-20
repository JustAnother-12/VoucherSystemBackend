package com.dacn.VoucherSystem.controller;

import com.dacn.VoucherSystem.BaseIntegrationTest;
import com.dacn.VoucherSystem.entity.BlackList;
import com.dacn.VoucherSystem.enums.BatchType;
import com.dacn.VoucherSystem.enums.BlacklistType;
import com.dacn.VoucherSystem.enums.CampaignStatus;
import com.dacn.VoucherSystem.fixture.RedemptionTestFixture;
import com.dacn.VoucherSystem.repository.BlackListRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class FraudLogControllerIntegrationTest extends BaseIntegrationTest {

    private static final String BASE_URL = "/fraud-logs";
    private static final String INDEX_KEY = "blacklist:active:index";

    @Autowired
    private RedemptionTestFixture testFixture;

    @Autowired
    private BlackListRepository blackListRepository;

    @Autowired
    private StringRedisTemplate redis;

    private RedemptionTestFixture.Fixture fixture;

    @BeforeEach
    void setUp() throws Exception {
        testFixture.cleanDatabase();
        redis.delete(INDEX_KEY);
        fixture = testFixture.createFixture(BatchType.GENERIC, CampaignStatus.ACTIVE, 1, 100);
    }

    @AfterEach
    void tearDown() {
        redis.delete(INDEX_KEY);
        testFixture.cleanDatabase();
    }

    @Test
    void manualFraudLogWithVoucherIsCreated() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .with(user("security").authorities(new SimpleGrantedAuthority("SECURITY_MANAGER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(fixture.voucherId(), "C-MANUAL", "MANUAL_REVIEW")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.violationRule").value("MANUAL_REVIEW"))
                .andExpect(jsonPath("$.data.voucherId").value(fixture.voucherId()))
                .andExpect(jsonPath("$.data.customerId").value("C-MANUAL"));
    }

    @Test
    void manualFraudLogWithoutVoucherIsCreated() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .with(user("security").authorities(new SimpleGrantedAuthority("SECURITY_MANAGER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"customerId":"C-NO-VOUCHER","ipAddress":"10.0.0.5",
                                 "deviceFingerprint":"DEV-5","violationRule":"MANUAL_REVIEW"}
                                """))
                .andExpect(status().isCreated());
    }

    @Test
    void manualFraudLogWithUnknownVoucherReturnsNotFound() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .with(user("security").authorities(new SimpleGrantedAuthority("SECURITY_MANAGER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(999999L, "C-X", "MANUAL_REVIEW")))
                .andExpect(status().isNotFound());
    }

    @Test
    void manualFraudLogValidationRejectsBlankCustomerAndNonPositiveVoucher() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .with(user("security").authorities(new SimpleGrantedAuthority("SECURITY_MANAGER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(fixture.voucherId(), " ", "MANUAL_REVIEW")))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post(BASE_URL)
                        .with(user("security").authorities(new SimpleGrantedAuthority("SECURITY_MANAGER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(0L, "C-ZERO", "MANUAL_REVIEW")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void fraudLogsRequireSecurityRole() throws Exception {
        mockMvc.perform(get(BASE_URL)
                        .with(user("campaign").authorities(new SimpleGrantedAuthority("CAMPAIGN_MANAGER"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void listAndGetByIdReturnCreatedLog() throws Exception {
        String response = mockMvc.perform(post(BASE_URL)
                        .with(user("security").authorities(new SimpleGrantedAuthority("SECURITY_MANAGER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(fixture.voucherId(), "C-LIST", "MANUAL_REVIEW")))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        long id = objectMapper.readTree(response).path("data").path("id").asLong();

        mockMvc.perform(get(BASE_URL).with(user("security").authorities(new SimpleGrantedAuthority("SECURITY_MANAGER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].customerId").value("C-LIST"));

        mockMvc.perform(get(BASE_URL + "/" + id).with(user("security").authorities(new SimpleGrantedAuthority("SECURITY_MANAGER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.violationRule").value("MANUAL_REVIEW"));

        mockMvc.perform(get(BASE_URL + "/999999").with(user("security").authorities(new SimpleGrantedAuthority("SECURITY_MANAGER"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void automatedFraudFromBlacklistAppearsInLogs() throws Exception {
        blackListRepository.saveAndFlush(BlackList.builder()
                .createdAt(LocalDateTime.now())
                .blacklistType(BlacklistType.CUSTOMER)
                .targetValue("C-AUTO")
                .reason("integration test")
                .isActive(true)
                .expiredAt(null)
                .build());
        redis.opsForZSet().add(INDEX_KEY, "CUSTOMER:" + sha("C-AUTO"), 9_007_199_254_740_991D);

        redeem(fixture.code(), "C-AUTO").andExpect(status().isForbidden());

        mockMvc.perform(get(BASE_URL).with(user("security").authorities(new SimpleGrantedAuthority("SECURITY_MANAGER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].violationRule").value("BLACKLIST_CUSTOMER"))
                .andExpect(jsonPath("$.data[0].customerId").value("C-AUTO"));
    }

    private String createBody(Long voucherId, String customerId, String rule) {
        return """
                {"voucherId":%d,"customerId":"%s","ipAddress":"10.0.0.5",
                 "deviceFingerprint":"DEV-5","violationRule":"%s"}
                """.formatted(voucherId, customerId, rule);
    }

    private ResultActions redeem(String code, String customerId) throws Exception {
        Map<String, Object> body = Map.of(
                "voucherCode", code,
                "customerId", customerId,
                "cartSubtotal", new BigDecimal("100.00"),
                "customerSegments", Set.of(),
                "items", List.of(Map.of(
                        "productId", "PRODUCT-01",
                        "quantity", 1,
                        "unitPrice", new BigDecimal("100.00")
                ))
        );

        return mockMvc.perform(post("/redemptions")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Device-Fingerprint", "DEVICE-" + customerId)
                .content(objectMapper.writeValueAsString(body))
                .with(req -> {
                    req.setRemoteAddr("127.0.0.1");
                    return req;
                }));
    }

    private static String sha(String value) {
        return com.dacn.VoucherSystem.util.Helpers.sha256(value);
    }
}