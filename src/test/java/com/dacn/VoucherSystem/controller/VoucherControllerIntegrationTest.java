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

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class VoucherControllerIntegrationTest extends BaseIntegrationTest {

    private static final String BASE_URL = "/vouchers";

    @Autowired
    private RedemptionTestFixture testFixture;

    private RedemptionTestFixture.Fixture active;
    private RedemptionTestFixture.Fixture ended;

    @BeforeEach
    void setUp() throws Exception {
        testFixture.cleanDatabase();
        active = testFixture.createFixture(BatchType.GENERIC, CampaignStatus.ACTIVE, 1, 5);
        ended = testFixture.createFixture(BatchType.GENERIC, CampaignStatus.ENDED, 1, 7);
    }

    @AfterEach
    void tearDown() {
        testFixture.cleanDatabase();
    }

    // ---------- danh sách ----------

    @Test
    void listWithoutFiltersReturnsAllVouchersPublicly() throws Exception {
        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)));
    }

    @Test
    void listFilterByBatchIdReturnsOnlyThatBatch() throws Exception {
        mockMvc.perform(get(BASE_URL).param("batchId", String.valueOf(active.batchId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].code").value(active.code()));
    }

    @Test
    void listFilterByCampaignIdReturnsOnlyThatCampaign() throws Exception {
        mockMvc.perform(get(BASE_URL).param("campaignId", String.valueOf(ended.campaignId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].code").value(ended.code()));
    }

    @Test
    void listFilterByBatchAndCampaignTogetherMustMatchBoth() throws Exception {
        mockMvc.perform(get(BASE_URL)
                        .param("batchId", String.valueOf(active.batchId()))
                        .param("campaignId", String.valueOf(active.campaignId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)));

        // batch của fixture này thuộc campaign khác -> không khớp
        mockMvc.perform(get(BASE_URL)
                        .param("batchId", String.valueOf(active.batchId()))
                        .param("campaignId", String.valueOf(ended.campaignId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));
    }

    @Test
    void listFilterWithUnknownIdReturnsEmptyList() throws Exception {
        mockMvc.perform(get(BASE_URL).param("batchId", "999999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));
    }

    // ---------- chi tiết theo id ----------

    @Test
    void getByIdReturnsVoucherWithCampaignBatchRewardAndCondition() throws Exception {
        mockMvc.perform(get(BASE_URL + "/" + active.voucherId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(active.voucherId()))
                .andExpect(jsonPath("$.data.code").value(active.code()))
                .andExpect(jsonPath("$.data.campaign.id").value(active.campaignId()))
                .andExpect(jsonPath("$.data.campaign.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.batch.id").value(active.batchId()))
                .andExpect(jsonPath("$.data.batch.batchType").value("GENERIC"))
                .andExpect(jsonPath("$.data.batch.maxUsageCount").value(5))
                .andExpect(jsonPath("$.data.reward.type").value("PERCENTAGE"))
                .andExpect(jsonPath("$.data.condition.type").value("MIN_CART_AMOUNT"));
    }

    @Test
    void getByIdReturnsEndedCampaignStatus() throws Exception {
        mockMvc.perform(get(BASE_URL + "/" + ended.voucherId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.campaign.status").value("ENDED"));
    }

    @Test
    void getByUnknownIdReturnsNotFound() throws Exception {
        mockMvc.perform(get(BASE_URL + "/999999"))
                .andExpect(status().isNotFound());
    }

    // ---------- chi tiết theo mã ----------

    @Test
    void getByCodeReturnsMatchingVoucher() throws Exception {
        mockMvc.perform(get(BASE_URL + "/code/" + active.code()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(active.voucherId()))
                .andExpect(jsonPath("$.data.code").value(active.code()));
    }

    @Test
    void getByUnknownCodeReturnsNotFound() throws Exception {
        mockMvc.perform(get(BASE_URL + "/code/NOT-EXISTS-CODE"))
                .andExpect(status().isNotFound());
    }
}