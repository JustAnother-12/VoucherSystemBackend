package com.dacn.VoucherSystem.controller;

import com.dacn.VoucherSystem.BaseIntegrationTest;
import com.dacn.VoucherSystem.entity.Condition;
import com.dacn.VoucherSystem.enums.ConditionType;
import com.dacn.VoucherSystem.enums.RuleStatus;
import com.dacn.VoucherSystem.fixture.RedemptionTestFixture;
import com.dacn.VoucherSystem.repository.ConditionRepository;
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
import static org.springframework.web.servlet.function.RequestPredicates.contentType;

@SpringBootTest
@AutoConfigureMockMvc
public class ConditionControllerIntegrationTest extends BaseIntegrationTest {

    private static final String BASE_URL = "/rule/conditions";

    @Autowired
    private RedemptionTestFixture testFixture;

    @Autowired
    private ConditionRepository conditionRepository;

    @BeforeEach
    void setUp() {
        testFixture.cleanDatabase();
    }

    @AfterEach
    void tearDown() {
        testFixture.cleanDatabase();
    }

    @Test
    void createValidConditionStartsAsDraft() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .with(user("it_admin").authorities(new SimpleGrantedAuthority("SYSTEM")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Min 50","conditionType":"MIN_CART_AMOUNT","config":{"minimumAmount":50,"amountBasis":"CART_SUBTOTAL"}}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").exists())
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andExpect(jsonPath("$.data.conditionType").value("MIN_CART_AMOUNT"));
    }

    @Test
    void createMinCartAmountWithoutRequiredFieldIsRejected() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .with(user("it_admin").authorities(new SimpleGrantedAuthority("SYSTEM")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Missing","conditionType":"MIN_CART_AMOUNT","config":{}}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createCompositeWithUnknownChildTypeIsRejected() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .with(user("it_admin").authorities(new SimpleGrantedAuthority("SYSTEM")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Bad composite","conditionType":"COMPOSITE","config":{
                                  "operator":"AND",
                                  "conditions":[{"type":"NOT_A_TYPE","config":{}}]
                                }}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateDraftConditionName() throws Exception {
        long id = createDraft("Old name");

        mockMvc.perform(patch(BASE_URL + "/" + id)
                        .with(user("it_admin").authorities(new SimpleGrantedAuthority("SYSTEM")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Renamed\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Renamed"));
    }

    @Test
    void updatePublishedConditionIsRejected() throws Exception {
        Condition published = conditionRepository.saveAndFlush(
                Condition.builder()
                        .createdAt(LocalDateTime.now())
                        .conditionName("Published")
                        .conditionType(ConditionType.MIN_CART_AMOUNT)
                        .conditionJson(objectMapper.readTree("{\"minimumAmount\": 50, \"amountBasis\": \"CART_SUBTOTAL\"}"))
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
    void updateDraftWithInvalidConfigIsRejected() throws Exception {
        long id = createDraft("Invalid update");

        mockMvc.perform(patch(BASE_URL + "/" + id)
                        .with(user("it_admin").authorities(new SimpleGrantedAuthority("SYSTEM")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"config\":{}}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getConditionsIsPublicAndUnknownIdReturnsNotFound() throws Exception {
        long id = createDraft("Public");

        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk());

        mockMvc.perform(get(BASE_URL + "/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Public"));

        mockMvc.perform(get(BASE_URL + "/999999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void createConditionWithoutAdminRoleIsForbidden() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .with(user("guest").authorities(new SimpleGrantedAuthority("SECURITY_MANAGER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"x","conditionType":"MIN_CART_AMOUNT","config":{"minimumAmount":50}}
                                """))
                .andExpect(status().isForbidden());
    }

    private long createDraft(String name) throws Exception {
        String response = mockMvc.perform(post(BASE_URL)
                        .with(user("it_admin").authorities(new SimpleGrantedAuthority("SYSTEM")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"%s","conditionType":"MIN_CART_AMOUNT","config":{"minimumAmount":50,"amountBasis":"CART_SUBTOTAL"}}
                                """.formatted(name)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response).path("data").path("id").asLong();
    }
}