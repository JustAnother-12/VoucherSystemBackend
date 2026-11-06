package com.dacn.VoucherSystem.controller;

import com.dacn.VoucherSystem.BaseIntegrationTest;
import com.dacn.VoucherSystem.entity.Reward;
import com.dacn.VoucherSystem.enums.BatchType;
import com.dacn.VoucherSystem.enums.CampaignStatus;
import com.dacn.VoucherSystem.enums.RewardType;
import com.dacn.VoucherSystem.enums.RuleStatus;
import com.dacn.VoucherSystem.fixture.RedemptionTestFixture;
import com.dacn.VoucherSystem.repository.BatchRuleRepository;
import com.dacn.VoucherSystem.repository.RewardRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.UUID;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class BatchRuleControllerIntegrationTest extends BaseIntegrationTest {

    private static final String BASE_URL = "/batch-rules";

    @Autowired
    private RedemptionTestFixture testFixture;

    @Autowired
    private BatchRuleRepository batchRuleRepository;

    @Autowired
    private RewardRepository rewardRepository;

    // Fixture có sẵn: 1 campaign, 1 rule, 1 batch GENERATED đang dùng rule đó
    private RedemptionTestFixture.Fixture existing;
    private Long ruleId;              // rule đang được batch sử dụng
    private Long existingRewardId;
    private Long existingConditionId;

    @BeforeEach
    void setUp() throws Exception {
        testFixture.cleanDatabase();
        existing = testFixture.createFixture(BatchType.GENERIC, CampaignStatus.ACTIVE);
        ruleId = batchRuleRepository.findAll().get(0).getId();

        // Lấy reward/condition của rule có sẵn qua API (tránh LazyInitializationException)
        String content = mockMvc.perform(get(BASE_URL + "/" + ruleId).with(auth()))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        existingRewardId = objectMapper.readTree(content).get("data").get("rewardId").asLong();
        existingConditionId = objectMapper.readTree(content).get("data").get("conditionId").asLong();
    }

    @AfterEach
    void tearDown() {
        testFixture.cleanDatabase();
    }

    private RequestPostProcessor auth() {
        return user("it_admin").authorities(new SimpleGrantedAuthority("SYSTEM"));
    }

    private Long createReward() {
        Reward reward = rewardRepository.saveAndFlush(
                Reward.builder()
                        .createdAt(LocalDateTime.now())
                        .rewardName("Reward test " + UUID.randomUUID())
                        .rewardType(RewardType.PERCENTAGE)
                        .rewardJson(objectMapper.readTree("""
                            {
                              "discountPercent": 10,
                              "maxDiscountAmount": 50
                            }
                            """))
                        .status(RuleStatus.PUBLISHED)
                        .build());
        return reward.getId();
    }

    private String ruleBody(Long rewardId, Long conditionId) {
        return """
                {"rewardId": %d, "conditionId": %d}
                """.formatted(rewardId, conditionId);
    }

    private Long createRuleViaApi(Long rewardId, Long conditionId) throws Exception {
        String content = mockMvc.perform(
                        post(BASE_URL)
                                .with(auth())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(ruleBody(rewardId, conditionId)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(content).get("data").get("id").asLong();
    }

    @Test
    void getAllBatchRulesSuccess() throws Exception {
        mockMvc.perform(get(BASE_URL).with(auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("Get Batch Rules successfully!"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(ruleId))
                .andExpect(jsonPath("$.data[0].rewardId").value(existingRewardId))
                .andExpect(jsonPath("$.data[0].conditionId").value(existingConditionId));

        // Filter đúng reward_id + condition_id: vẫn ra 1 rule
        mockMvc.perform(get(BASE_URL)
                        .with(auth())
                        .param("reward_id", String.valueOf(existingRewardId))
                        .param("condition_id", String.valueOf(existingConditionId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(ruleId));

        // Filter reward_id không tồn tại: danh sách rỗng
        mockMvc.perform(get(BASE_URL)
                        .with(auth())
                        .param("reward_id", "999999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void getBatchRuleByIdSuccess() throws Exception {
        mockMvc.perform(get(BASE_URL + "/" + ruleId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("Get Batch Rules successfully!"))
                .andExpect(jsonPath("$.data.id").value(ruleId))
                .andExpect(jsonPath("$.data.rewardId").value(existingRewardId))
                .andExpect(jsonPath("$.data.conditionId").value(existingConditionId));
    }

    @Test
    void createBatchRuleSuccess() throws Exception {
        Long newRewardId = createReward();

        mockMvc.perform(
                        post(BASE_URL)
                                .with(auth())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(ruleBody(newRewardId, existingConditionId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.message").value("Create Batch Rule successfully!"))
                .andExpect(jsonPath("$.data.id").isNumber())
                .andExpect(jsonPath("$.data.rewardId").value(newRewardId))
                .andExpect(jsonPath("$.data.conditionId").value(existingConditionId));

        assertTrue(batchRuleRepository.existsByRewardIdAndConditionId(
                newRewardId, existingConditionId));
    }

    @Test
    void updateBatchRuleSuccess() throws Exception {
        Long rewardA = createReward();
        Long rewardB = createReward();
        Long newRuleId = createRuleViaApi(rewardA, existingConditionId);

        mockMvc.perform(
                        patch(BASE_URL + "/" + newRuleId)
                                .with(auth())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"rewardId": %d}
                                        """.formatted(rewardB)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("Update Batch Rule successfully!"))
                .andExpect(jsonPath("$.data.id").value(newRuleId))
                .andExpect(jsonPath("$.data.rewardId").value(rewardB))
                .andExpect(jsonPath("$.data.conditionId").value(existingConditionId));

        mockMvc.perform(get(BASE_URL + "/" + newRuleId).with(auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.rewardId").value(rewardB))
                .andExpect(jsonPath("$.data.conditionId").value(existingConditionId));
    }

    @Test
    void updateBatchRuleFailedWhenRuleInUse() throws Exception {
        Long newRewardId = createReward();

        mockMvc.perform(
                        patch(BASE_URL + "/" + ruleId)
                                .with(auth())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"rewardId": %d}
                                        """.formatted(newRewardId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").isNotEmpty());

        mockMvc.perform(get(BASE_URL + "/" + ruleId).with(auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.rewardId").value(existingRewardId))
                .andExpect(jsonPath("$.data.conditionId").value(existingConditionId));
    }

    @Test
    void updateBatchRuleFailedWhenStatusVoucherBatchIsGenerated() throws Exception {
        Long newRewardId = createReward();

        mockMvc.perform(
                        patch(BASE_URL + "/" + ruleId)
                                .with(auth())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"rewardId": %d}
                                        """.formatted(newRewardId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Cannot update a rule that is currently in use by a generated voucher batch."));

        mockMvc.perform(get(BASE_URL + "/" + ruleId).with(auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.rewardId").value(existingRewardId))
                .andExpect(jsonPath("$.data.conditionId").value(existingConditionId));
    }


}