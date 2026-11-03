package com.dacn.VoucherSystem.controller;

import com.dacn.VoucherSystem.BaseIntegrationTest;
import com.dacn.VoucherSystem.entity.AdminAccount;
import com.dacn.VoucherSystem.entity.Campaign;
import com.dacn.VoucherSystem.enums.BatchType;
import com.dacn.VoucherSystem.enums.CampaignStatus;
import com.dacn.VoucherSystem.fixture.RedemptionTestFixture;
import com.dacn.VoucherSystem.repository.AdminAccountRepository;
import com.dacn.VoucherSystem.repository.CampaignRepository;
import com.dacn.VoucherSystem.security.CustomUserDetails;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class CampaignControllerIntegrationTest extends BaseIntegrationTest {

    private static final String BASE_URL = "/campaigns";

    @Autowired
    private RedemptionTestFixture testFixture;

    @Autowired
    private CampaignRepository campaignRepository;

    @Autowired
    private AdminAccountRepository adminAccountRepository;

    // Fixture có sẵn: 1 campaign (ACTIVE), 1 rule, 1 batch
    private RedemptionTestFixture.Fixture existing;

    @BeforeEach
    void setUp() throws Exception {
        testFixture.cleanDatabase();
        existing = testFixture.createFixture(BatchType.GENERIC, CampaignStatus.ACTIVE);
    }

    @AfterEach
    void tearDown() {
        testFixture.cleanDatabase();
    }

    private RequestPostProcessor auth() {
        return user("it_admin").authorities(new SimpleGrantedAuthority("SYSTEM"));
    }

    private RequestPostProcessor authWithAdmin() {
        AdminAccount admin = adminAccountRepository.findAll().stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Fixture chưa tạo AdminAccount"));

        CustomUserDetails principal = new CustomUserDetails(admin);
        return user(principal);
    }

    private String createBody(String name, String start, String end) {
        return """
                {
                  "name": "%s",
                  "startTime": "%s",
                  "endTime": "%s"
                }
                """.formatted(name, start, end);
    }

    @Test
    void getCampaignsSuccess() throws Exception {
        mockMvc.perform(get(BASE_URL).with(auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("Get campaigns successfully!"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(existing.campaignId()));
    }

    @Test
    void getCampaignByIDSuccess() throws Exception {
        mockMvc.perform(get(BASE_URL + "/" + existing.campaignId()).with(auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("Get campaign successfully!"))
                .andExpect(jsonPath("$.data.id").value(existing.campaignId()));
    }

    @Test
    void createCampaignSuccess() throws Exception {
        String content = mockMvc.perform(
                        post(BASE_URL)
                                .with(authWithAdmin())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(createBody(
                                        "New Campaign",
                                        "2027-01-01T00:00:00",
                                        "2027-02-01T00:00:00")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.message").value("Create campaign successfully!"))
                .andExpect(jsonPath("$.data.name").value("New Campaign"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long newId = objectMapper.readTree(content).get("data").get("id").asLong();

        Campaign saved = campaignRepository.findById(newId).orElseThrow();
        assertEquals("New Campaign", saved.getCampaignName());
    }

    @Test
    void updateCampaignSuccess() throws Exception {
        Campaign before = campaignRepository.findById(existing.campaignId()).orElseThrow();

        mockMvc.perform(
                        patch(BASE_URL + "/" + existing.campaignId())
                                .with(auth())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"name": "Updated Name"}
                                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("Update condition successfully!"))
                .andExpect(jsonPath("$.data.name").value("Updated Name"));

        Campaign after = campaignRepository.findById(existing.campaignId()).orElseThrow();
        assertEquals("Updated Name", after.getCampaignName());
        assertEquals(before.getStartTime(), after.getStartTime());
        assertEquals(before.getEndTime(), after.getEndTime());
    }

    @Test
    void createCampaignFailedValidateTime() throws Exception {
        long before = campaignRepository.count();

        mockMvc.perform(
                        post(BASE_URL)
                                .with(authWithAdmin())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(createBody(
                                        "Invalid Campaign",
                                        "2027-02-01T00:00:00",
                                        "2027-01-01T00:00:00")))
                .andExpect(status().isBadRequest());

        assertEquals(before, campaignRepository.count());
        assertTrue(campaignRepository.findAll().stream()
                .noneMatch(c -> "Invalid Campaign".equals(c.getCampaignName())));
    }

    @Test
    void updateCampaignFailedValidateTime() throws Exception {
        Campaign before = campaignRepository.findById(existing.campaignId()).orElseThrow();

        mockMvc.perform(
                        patch(BASE_URL + "/" + existing.campaignId())
                                .with(auth())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "startTime": "2027-02-01T00:00:00",
                                          "endTime": "2027-01-01T00:00:00"
                                        }
                                        """))
                .andExpect(status().isBadRequest());

        Campaign after = campaignRepository.findById(existing.campaignId()).orElseThrow();
        assertEquals(before.getStartTime(), after.getStartTime());
        assertEquals(before.getEndTime(), after.getEndTime());
    }
}