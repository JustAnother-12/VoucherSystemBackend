package com.dacn.VoucherSystem.controller;

import com.dacn.VoucherSystem.BaseIntegrationTest;
import com.dacn.VoucherSystem.entity.Reward;
import com.dacn.VoucherSystem.enums.RewardType;
import com.dacn.VoucherSystem.enums.RuleStatus;
import com.dacn.VoucherSystem.fixture.RedemptionTestFixture;
import com.dacn.VoucherSystem.repository.RewardRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.LocalDateTime;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class RewardControllerIntegrationTest extends BaseIntegrationTest {

    private static final String BASE_URL = "/rule/rewards";

    @Autowired
    private RedemptionTestFixture testFixture;

    @Autowired
    private RewardRepository rewardRepository;

    @BeforeEach
    void setUp() {
        testFixture.cleanDatabase();
    }

    @AfterEach
    void tearDown() {
        testFixture.cleanDatabase();
    }

    @Test
    void createValidPercentageRewardStartsAsDraft() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .with(user("it_admin").authorities(new SimpleGrantedAuthority("SYSTEM")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("Pct", "PERCENTAGE",
                                "{\"discountPercent\":10,\"maxDiscountAmount\":50,\"amountBasis\":\"CART_SUBTOTAL\"}")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andExpect(jsonPath("$.data.rewardType").value("PERCENTAGE"));
    }

    @Test
    void percentageOutOfRangeIsRejected() throws Exception {
        assertBadRequest("PERCENTAGE",
                "{\"discountPercent\":150,\"maxDiscountAmount\":50,\"amountBasis\":\"CART_SUBTOTAL\"}");
    }

    @Test
    void percentageWithUnsupportedAmountBasisIsRejected() throws Exception {
        assertBadRequest("PERCENTAGE",
                "{\"discountPercent\":10,\"maxDiscountAmount\":50,\"amountBasis\":\"PRODUCT\"}");
    }

    @Test
    void fixedRewardWithZeroValueIsRejected() throws Exception {
        assertBadRequest("FIXED", "{\"discountValue\":0}");
    }

    @Test
    void tieredRewardWithNonIncreasingTiersIsRejected() throws Exception {
        assertBadRequest("TIERED_PERCENTAGE", """
                {"calculationMode":"HIGHEST_MATCH","amountBasis":"CART_SUBTOTAL","tiers":[
                  {"minCartAmount":200,"discountPercent":20,"maxDiscountAmount":100},
                  {"minCartAmount":50,"discountPercent":10,"maxDiscountAmount":20}
                ]}
                """);
    }

    @Test
    void buyXGetYWithUnsupportedCalculationModeIsRejected() throws Exception {
        assertBadRequest("BUY_X_GET_Y", """
                {"calculationMode":"PER_ORDER",
                 "buy":{"productIds":["A"],"quantity":2},
                 "get":{"productIds":["B"],"quantity":1,"discountPercent":50},
                 "maxApplicationsPerOrder":2}
                """);
    }

    @Test
    void updateDraftRewardName() throws Exception {
        long id = createDraft();

        mockMvc.perform(patch(BASE_URL + "/" + id)
                        .with(user("it_admin").authorities(new SimpleGrantedAuthority("SYSTEM")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Renamed\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Renamed"));
    }

    @Test
    void updatePublishedRewardIsRejected() throws Exception {
        Reward published = rewardRepository.saveAndFlush(
                Reward.builder()
                        .createdAt(LocalDateTime.now())
                        .rewardName("Published")
                        .rewardType(RewardType.FIXED)
                        .rewardJson(objectMapper.readTree("{\"discountValue\": 10}"))
                        .status(RuleStatus.PUBLISHED)
                        .build()
        );

        mockMvc.perform(patch(BASE_URL + "/" + published.getId())
                        .with(user("it_admin").authorities(new SimpleGrantedAuthority("SYSTEM")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Should not change\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getRewardsIsPublicAndUnknownIdReturnsNotFound() throws Exception {
        long id = createDraft();

        mockMvc.perform(get(BASE_URL)).andExpect(status().isOk());
        mockMvc.perform(get(BASE_URL + "/" + id)).andExpect(status().isOk());
        mockMvc.perform(get(BASE_URL + "/999999")).andExpect(status().isNotFound());
    }

    @Test
    void createRewardWithoutRuleRoleIsForbidden() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .with(user("guest").authorities(new SimpleGrantedAuthority("SECURITY_MANAGER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("X", "FIXED", "{\"discountValue\":10}")))
                .andExpect(status().isForbidden());
    }

    private String body(String name, String type, String config) {
        return """
                {"name":"%s","rewardType":"%s","config":%s}
                """.formatted(name, type, config);
    }

    private void assertBadRequest(String type, String config) throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .with(user("it_admin").authorities(new SimpleGrantedAuthority("SYSTEM")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("Invalid", type, config)))
                .andExpect(status().isBadRequest());
    }

    private long createDraft() throws Exception {
        String response = mockMvc.perform(post(BASE_URL)
                        .with(user("it_admin").authorities(new SimpleGrantedAuthority("SYSTEM")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("Draft", "FIXED", "{\"discountValue\":10}")))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response).path("data").path("id").asLong();
    }
}