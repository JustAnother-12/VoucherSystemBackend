package com.dacn.VoucherSystem.controller;

import com.dacn.VoucherSystem.BaseIntegrationTest;
import com.dacn.VoucherSystem.fixture.StatisticTestFixture;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
public class StatisticControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private StatisticTestFixture testFixture;

    private StatisticTestFixture.Fixture fixture;

    @BeforeEach
    void setUp() {
        testFixture.cleanDatabase();
        fixture = testFixture.createFixture();
    }

    @AfterEach
    void tearDown() {
        testFixture.cleanDatabase();
    }

    private ResultActions getRedemptionRate(
            String campaignId,
            String batchId,
            Boolean includeCodes
    ) throws Exception {
        var requestBuilder = get("/statistic/campaigns/" + campaignId + "/redemption-rate")
                .contentType(MediaType.APPLICATION_JSON);

        if (batchId != null) {
            requestBuilder.param("batchId", batchId);
        }
        if (includeCodes != null) {
            requestBuilder.param("includeCodes", includeCodes.toString());
        }

        return mockMvc.perform(requestBuilder);
    }

    private ResultActions getIncrementalRevenue(
            String campaignId,
            String batchId,
            Boolean includeCodes
    ) throws Exception {
        var requestBuilder = get("/statistic/campaigns/" + campaignId + "/incremental-revenue")
                .contentType(MediaType.APPLICATION_JSON);

        if (batchId != null) {
            requestBuilder.param("batchId", batchId);
        }
        if (includeCodes != null) {
            requestBuilder.param("includeCodes", includeCodes.toString());
        }

        return mockMvc.perform(requestBuilder);
    }

    @Test
    @DisplayName("Lấy chỉ số Redemption Rate thành công (includeCodes = false)")
    void getRedemptionRateSuccessfully_WithoutCodes() throws Exception {
        getRedemptionRate(String.valueOf(fixture.campaignId()), null, false)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("Get campaign redemption rate statistics successfully!"))
                .andExpect(jsonPath("$.data.campaignId").value(String.valueOf(fixture.campaignId())))
                .andExpect(jsonPath("$.data.campaignName").value(fixture.campaignName()))
                .andExpect(jsonPath("$.data.campaignSummary.totalIssued").value(20))
                .andExpect(jsonPath("$.data.campaignSummary.totalRedeemed").value(2))
                .andExpect(jsonPath("$.data.campaignSummary.redemptionRate").value(10.0))
                .andExpect(jsonPath("$.data.batches[0].codes").isEmpty());

        assertEquals(2, testFixture.countSuccessLogsByCampaign(fixture.campaignId()));
    }

    @Test
    @DisplayName("Lấy chỉ số Redemption Rate thành công (includeCodes = true)")
    void getRedemptionRateSuccessfully_WithCodes() throws Exception {
        getRedemptionRate(String.valueOf(fixture.campaignId()), null, true)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.batches[0].codes", hasSize(20)))
                .andExpect(jsonPath("$.data.batches[0].codes[0].code").value("BULK_CODE_1"))
                .andExpect(jsonPath("$.data.batches[0].codes[0].redeemedCount").value(1))
                .andExpect(jsonPath("$.data.batches[0].codes[0].redemptionRate").value(100.0))
                .andExpect(jsonPath("$.data.batches[0].codes[2].code").value("BULK_CODE_3"))
                .andExpect(jsonPath("$.data.batches[0].codes[2].redeemedCount").value(0))
                .andExpect(jsonPath("$.data.batches[0].codes[2].redemptionRate").value(0.0));
    }

    @Test
    @DisplayName("Lấy chỉ số Incremental Revenue thành công (includeCodes = false)")
    void getIncrementalRevenueSuccessfully_WithoutCodes() throws Exception {
        getIncrementalRevenue(String.valueOf(fixture.campaignId()), null, false)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("Get campaign incremental revenue statistics successfully!"))
                .andExpect(jsonPath("$.data.campaignId").value(String.valueOf(fixture.campaignId())))
                .andExpect(jsonPath("$.data.campaignSummary.totalOriginalAmount").value(1300000))
                .andExpect(jsonPath("$.data.campaignSummary.totalDiscountAmount").value(130000))
                .andExpect(jsonPath("$.data.campaignSummary.totalFinalAmount").value(1170000))
                .andExpect(jsonPath("$.data.campaignSummary.roi").value(900.0))
                .andExpect(jsonPath("$.data.batches[0].codes").isEmpty());
    }

    @Test
    @DisplayName("Lấy chỉ số Incremental Revenue thành công (lọc theo BatchId và includeCodes = true)")
    void getIncrementalRevenueSuccessfully_WithBatchFilterAndCodes() throws Exception {
        getIncrementalRevenue(
                String.valueOf(fixture.campaignId()),
                String.valueOf(fixture.batchId()),
                true
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.batches", hasSize(1)))
                .andExpect(jsonPath("$.data.batches[0].batchId").value(String.valueOf(fixture.batchId())))
                .andExpect(jsonPath("$.data.batches[0].codes", hasSize(20)))
                .andExpect(jsonPath("$.data.batches[0].codes[0].code").value("BULK_CODE_1"))
                .andExpect(jsonPath("$.data.batches[0].codes[0].totalOriginalAmount").value(1000000))
                .andExpect(jsonPath("$.data.batches[0].codes[0].roi").value(900.0));
    }

    @Test
    @DisplayName("Trả về HTTP 404 khi Campaign ID không tồn tại trong Database")
    void getIncrementalRevenue_NotFound() throws Exception {
        getIncrementalRevenue("999999", null, false)
                .andExpect(status().isNotFound());
    }
}
