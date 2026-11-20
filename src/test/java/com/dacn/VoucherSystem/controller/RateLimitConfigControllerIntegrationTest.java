package com.dacn.VoucherSystem.controller;

import com.dacn.VoucherSystem.BaseIntegrationTest;
import com.dacn.VoucherSystem.cache.RateLimitPolicyCache;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class RateLimitConfigControllerIntegrationTest extends BaseIntegrationTest {

    private static final String BASE_URL = "/rate-limit-configs";
    private static final String INDEX_KEY = "blacklist:active:index";

    @Autowired
    private RedemptionTestFixture testFixture;

    @Autowired
    private RateLimitPolicyCache policyCache;

    @Autowired
    private StringRedisTemplate redis;

    private RedemptionTestFixture.Fixture fixture;

    @BeforeEach
    void setUp() throws Exception {
        testFixture.cleanDatabase();
        policyCache.invalidate();
        redis.delete(INDEX_KEY);
        fixture = testFixture.createFixture(BatchType.GENERIC, CampaignStatus.ACTIVE, 1, 100);
    }

    @AfterEach
    void tearDown() {
        // cleanDatabase không xóa cache trong bộ nhớ, nên phải invalidate để policy không lan sang test khác
        policyCache.invalidate();
        redis.delete(INDEX_KEY);
        testFixture.cleanDatabase();
    }

    // ---------- CRUD ----------

    @Test
    void createConfigIsActiveByDefault() throws Exception {
        createPolicy("Redeem limit", "CUSTOMER_ID", 2, "REJECT_429")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.isActive").value(true))
                .andExpect(jsonPath("$.data.scope").value("CUSTOMER_ID"))
                .andExpect(jsonPath("$.data.maxRequest").value(2));
    }

    @Test
    void createConfigValidatesRequiredFieldsAndPositiveValues() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .with(user("it_admin").authorities(new SimpleGrantedAuthority("SYSTEM")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(policyBody("Zero", "CUSTOMER_ID", 0, "REJECT_429")))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post(BASE_URL)
                        .with(user("it_admin").authorities(new SimpleGrantedAuthority("SYSTEM")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"No action","endpointPattern":"/redemptions",
                                 "limitScope":"CUSTOMER_ID","maxRequest":2,"timeWindowSeconds":600,"priority":1}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateConfigChangesFieldsAndActiveFlag() throws Exception {
        long id = createPolicyAndGetId("Editable", "CUSTOMER_ID", 2, "REJECT_429");

        mockMvc.perform(patch(BASE_URL + "/" + id)
                        .with(user("it_admin").authorities(new SimpleGrantedAuthority("SYSTEM")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"maxRequest\":5,\"isActive\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.maxRequest").value(5))
                .andExpect(jsonPath("$.data.isActive").value(false));
    }

    @Test
    void getConfigsRequiresSecurityRoleAndUnknownIdReturnsNotFound() throws Exception {
        long id = createPolicyAndGetId("Visible", "CUSTOMER_ID", 2, "REJECT_429");

        mockMvc.perform(get(BASE_URL).with(user("it_admin").authorities(new SimpleGrantedAuthority("SYSTEM"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("Visible"));

        mockMvc.perform(get(BASE_URL + "/" + id).with(user("it_admin").authorities(new SimpleGrantedAuthority("SYSTEM"))))
                .andExpect(status().isOk());

        mockMvc.perform(get(BASE_URL + "/999999").with(user("it_admin").authorities(new SimpleGrantedAuthority("SYSTEM"))))
                .andExpect(status().isNotFound());

        mockMvc.perform(get(BASE_URL)
                        .with(user("campaign").authorities(new SimpleGrantedAuthority("CAMPAIGN_MANAGER"))))
                .andExpect(status().isForbidden());
    }

    // ---------- hành vi khi áp dụng vào /redemptions ----------

    @Test
    void rejectPolicyReturns429WhenLimitExceeded() throws Exception {
        createPolicy("Reject", "CUSTOMER_ID", 2, "REJECT_429")
                .andExpect(status().isCreated());

        String customer = uniqueCustomer();

        int first = redeem(customer).andReturn().getResponse().getStatus();
        int second = redeem(customer).andReturn().getResponse().getStatus();
        int third = redeem(customer).andReturn().getResponse().getStatus();

        assertEquals(200, first);
        assertNotEquals(429, second); // còn trong hạn mức (lần 2 bị 409 vì đã đổi voucher)
        assertEquals(429, third);
    }

    @Test
    void blacklistPolicyCreatesTemporaryBlacklistAndFraudLog() throws Exception {
        createPolicy("Blacklist", "CUSTOMER_ID", 1, "BLACKLIST").andExpect(status().isCreated());

        String customer = uniqueCustomer();

        redeem(customer).andExpect(status().isOk());
        redeem(customer).andExpect(status().isForbidden());

        assertEquals(1, countFraudLogs("EXCEEDED_RATE_LIMIT", customer));
        assertEquals(1, countTemporaryBlacklists(customer));

        // Lần sau vẫn bị chặn và vẫn ghi log vượt hạn mức
        redeem(customer).andExpect(status().isForbidden());
        assertEquals(2, countFraudLogs("EXCEEDED_RATE_LIMIT", customer));
        assertEquals(1, countTemporaryBlacklists(customer));
    }

    @Test
    void deactivatedPolicyDoesNotLimitRequests() throws Exception {
        long id = createPolicyAndGetId("Toggle", "CUSTOMER_ID", 1, "REJECT_429");

        mockMvc.perform(patch(BASE_URL + "/" + id)
                        .with(user("it_admin").authorities(new SimpleGrantedAuthority("SYSTEM")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"isActive\":false}"))
                .andExpect(status().isOk());

        String customer = uniqueCustomer();
        for (int i = 0; i < 3; i++) {
            int status = redeem(customer).andReturn().getResponse().getStatus();
            assertNotEquals(429, status);
        }
    }

    private String uniqueCustomer() {
        return "RL-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private String policyBody(String name, String scope, int maxRequest, String action) {
        return """
                {"name":"%s","endpointPattern":"/redemptions","limitScope":"%s",
                 "maxRequest":%d,"timeWindowSeconds":600,"actionOnExceed":"%s","priority":1}
                """.formatted(name, scope, maxRequest, action);
    }

    private ResultActions createPolicy(String name, String scope, int maxRequest, String action) throws Exception {
        return mockMvc.perform(post(BASE_URL)
                .with(user("it_admin").authorities(new SimpleGrantedAuthority("SYSTEM")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(policyBody(name, scope, maxRequest, action)));
    }

    private long createPolicyAndGetId(String name, String scope, int maxRequest, String action) throws Exception {
        String response = createPolicy(name, scope, maxRequest, action)
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).path("data").path("id").asLong();
    }

    private ResultActions redeem(String customerId) throws Exception {
        Map<String, Object> body = Map.of(
                "voucherCode", fixture.code(),
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

    private int countFraudLogs(String rule, String customerId) {
        return testFixture.jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM fraud_logs WHERE violation_rule = ? AND customer_id = ?",
                Integer.class, rule, customerId);
    }

    private int countTemporaryBlacklists(String customerId) {
        return testFixture.jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*) FROM black_lists
                WHERE blacklist_type = 'CUSTOMER' AND target_value = ?
                  AND reason = 'Automatically blocked by rate limiter'
                """,
                Integer.class, customerId);
    }
}