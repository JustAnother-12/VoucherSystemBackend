package com.dacn.VoucherSystem.controller;

import com.dacn.VoucherSystem.BaseIntegrationTest;
import com.dacn.VoucherSystem.enums.BatchType;
import com.dacn.VoucherSystem.enums.CampaignStatus;
import com.dacn.VoucherSystem.fixture.RedemptionTestFixture;
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
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class BlackListIntegrationTest extends BaseIntegrationTest {

    private static final String BASE_URL = "/black-lists";
    private static final String INDEX_KEY = "blacklist:active:index";

    @Autowired
    private RedemptionTestFixture testFixture;

    @Autowired
    private StringRedisTemplate redis;

    private RedemptionTestFixture.Fixture fixture;

    @BeforeEach
    void setUp() throws Exception {
        testFixture.cleanDatabase();
        redis.delete(INDEX_KEY);

        fixture = testFixture.createFixture(
                BatchType.GENERIC,
                CampaignStatus.ACTIVE,
                1,
                100
        );
    }

    @AfterEach
    void tearDown() {
        redis.delete(INDEX_KEY);
        testFixture.cleanDatabase();
    }

    @Test
    void blacklistedCustomerIsRejectedAndNoRedemptionLogIsWritten() throws Exception {
        createBlacklist("CUSTOMER", "CUSTOMER-BLOCKED", "null");

        redeem(fixture.code(), "CUSTOMER-BLOCKED", "127.0.0.1", "DEVICE-1")
                .andExpect(status().isForbidden());

        assertEquals(1, countFraudLogs("BLACKLIST_CUSTOMER", "CUSTOMER-BLOCKED"));
        assertEquals(0, countRedemptionLogs("CUSTOMER-BLOCKED"));
    }

    @Test
    void blacklistedIpBlocksAnyCustomerFromThatIpOnly() throws Exception {
        createBlacklist("IP", "10.0.0.99", "null");

        redeem(fixture.code(), "CUSTOMER-A", "10.0.0.99", "DEVICE-A")
                .andExpect(status().isForbidden());
        assertEquals(1, countFraudLogs("BLACKLIST_IP", "CUSTOMER-A"));

        redeem(fixture.code(), "CUSTOMER-B", "10.0.0.1", "DEVICE-B")
                .andExpect(status().isOk());
    }

    @Test
    void blacklistedDeviceFingerprintIsRejected() throws Exception {
        createBlacklist("DEVICE_FINGERPRINT", "DEVICE-BAD", "null");

        redeem(fixture.code(), "CUSTOMER-C", "10.0.0.2", "DEVICE-BAD")
                .andExpect(status().isForbidden());
        assertEquals(1, countFraudLogs("BLACKLIST_DEVICE_FINGERPRINT", "CUSTOMER-C"));

        redeem(fixture.code(), "CUSTOMER-C", "10.0.0.2", "DEVICE-GOOD")
                .andExpect(status().isOk());
    }

    @Test
    void deactivatingBlacklistRestoresAccessAndReactivatingBlocksAgain() throws Exception {
        long id = createBlacklist("CUSTOMER", "CUSTOMER-TOGGLE", "null");

        patchBlacklist(id, "{\"isActive\":false}")
                .andExpect(status().isOk());
        redeem(fixture.code(), "CUSTOMER-TOGGLE", "127.0.0.1", "DEVICE-T")
                .andExpect(status().isOk());

        patchBlacklist(id, "{\"isActive\":true}")
                .andExpect(status().isOk());
        redeem(fixture.code(), "CUSTOMER-TOGGLE", "127.0.0.1", "DEVICE-T")
                .andExpect(status().isForbidden());
    }

    @Test
    void blacklistStopsBlockingAfterExpiredAt() throws Exception {
        LocalDateTime soon = LocalDateTime.now(testFixture.clock).plusSeconds(3).withNano(0);
        createBlacklist("CUSTOMER", "CUSTOMER-TEMP", "\"" + soon + "\"");

        redeem(fixture.code(), "CUSTOMER-TEMP", "127.0.0.1", "DEVICE-TEMP")
                .andExpect(status().isForbidden());

        Thread.sleep(4000);

        redeem(fixture.code(), "CUSTOMER-TEMP", "127.0.0.1", "DEVICE-TEMP")
                .andExpect(status().isOk());
    }

    @Test
    void duplicateActiveBlacklistReturnsConflict() throws Exception {
        createBlacklist("CUSTOMER", "CUSTOMER-DUP", "null");

        mockMvc.perform(post(BASE_URL)
                        .with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody("CUSTOMER", "CUSTOMER-DUP", "null")))
                .andExpect(status().isConflict());
    }

    @Test
    void createBlacklistRequiresNonBlankReason() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"blacklistType\":\"IP\",\"targetValue\":\"1.1.1.1\",\"reason\":\" \"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createBlacklistWithPastExpirationIsRejected() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody("CUSTOMER", "CUSTOMER-PAST", "\"2020-01-01T00:00:00\"")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void userWithoutSecurityRoleCannotManageBlacklist() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .with(user("campaign-user")
                                .authorities(new SimpleGrantedAuthority("CAMPAIGN_MANAGER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody("IP", "10.9.9.9", "null")))
                .andExpect(status().isForbidden());
    }

    @Test
    void listAndGetByIdReturnCreatedBlacklist() throws Exception {
        long id = createBlacklist("CUSTOMER", "CUSTOMER-LIST", "null");

        mockMvc.perform(get(BASE_URL).with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].value").value("CUSTOMER-LIST"));

        mockMvc.perform(get(BASE_URL + "/" + id).with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.type").value("CUSTOMER"));

        mockMvc.perform(get(BASE_URL + "/999999").with(admin()))
                .andExpect(status().isNotFound());
    }

    // ---------- helpers ----------

    private RequestPostProcessor admin() {
        return user("it_admin").authorities(new SimpleGrantedAuthority("SYSTEM"));
    }

    private String createBody(String type, String value, String expiredAtJson) {
        return """
                {"blacklistType":"%s","targetValue":"%s","reason":"integration test","expiredAt":%s}
                """.formatted(type, value, expiredAtJson);
    }

    private long createBlacklist(String type, String value, String expiredAtJson) throws Exception {
        String response = mockMvc.perform(post(BASE_URL)
                        .with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(type, value, expiredAtJson)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response).path("data").path("id").asLong();
    }

    private ResultActions patchBlacklist(long id, String body) throws Exception {
        return mockMvc.perform(patch(BASE_URL + "/" + id)
                .with(admin())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private ResultActions redeem(String code, String customerId, String ip, String device) throws Exception {
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
                .header("X-Device-Fingerprint", device)
                .content(objectMapper.writeValueAsString(body))
                .with(req -> {
                    req.setRemoteAddr(ip);
                    return req;
                }));
    }

    private int countFraudLogs(String violationRule, String customerId) {
        return testFixture.jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM fraud_logs WHERE violation_rule = ? AND customer_id = ?",
                Integer.class,
                violationRule,
                customerId
        );
    }

    private int countRedemptionLogs(String customerId) {
        return testFixture.jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM redemption_logs WHERE customer_id = ?",
                Integer.class,
                customerId
        );
    }
}