package com.dacn.VoucherSystem.controller;

import com.dacn.VoucherSystem.BaseIntegrationTest;
import com.dacn.VoucherSystem.entity.VoucherBatch;
import com.dacn.VoucherSystem.enums.BatchType;
import com.dacn.VoucherSystem.enums.CampaignStatus;
import com.dacn.VoucherSystem.enums.RuleStatus;
import com.dacn.VoucherSystem.fixture.RedemptionTestFixture;
import com.dacn.VoucherSystem.repository.BatchRuleRepository;
import com.dacn.VoucherSystem.repository.VoucherBatchRepository;
import jakarta.transaction.Transactional;
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
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class VoucherBatchControllerIntegrationTest extends BaseIntegrationTest {

    private static final String BASE_URL = "/voucher-batches";

    private static final String CODE_CONFIG = """
            {"prefix": "TEST", "length": 8, "charset": "ALPHANUMERIC"}
            """;

    private static final String CHANNELS = """
            {"allocations": [
                {"channel": "EMAIL", "quota": 3},
                {"channel": "WEBHOOK", "quota": 2}
            ]}
            """;

    @Autowired
    private RedemptionTestFixture testFixture;

    @Autowired
    private BatchRuleRepository batchRuleRepository;

    @Autowired
    private VoucherBatchRepository voucherBatchRepository;

    // Fixture có sẵn: 1 campaign, 1 rule, 1 batch ở trạng thái GENERATED
    private RedemptionTestFixture.Fixture existing;
    private Long ruleId;

    @BeforeEach
    void setUp() throws Exception {
        testFixture.cleanDatabase();
        existing = testFixture.createFixture(BatchType.GENERIC, CampaignStatus.ACTIVE);
        ruleId = batchRuleRepository.findAll().get(0).getId();
    }

    @AfterEach
    void tearDown() {
        testFixture.cleanDatabase();
    }

    private RequestPostProcessor auth() {
        return user("it_admin").authorities(new SimpleGrantedAuthority("SYSTEM"));
    }

    private String createBody(BatchType type) {
        return """
                {
                  "campaignId": %d,
                  "ruleId": %d,
                  "batchType": "%s",
                  "codeConfig": %s,
                  "durationUnit": "DAYS",
                  "durationValue": 30,
                  "distribution_channels": %s
                }
                """.formatted(existing.campaignId(), ruleId, type.name(), CODE_CONFIG, CHANNELS);
    }

    private Long createDraftBatch(BatchType type) throws Exception {
        String content = mockMvc.perform(
                        post(BASE_URL)
                                .with(auth())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(createBody(type)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(content).get("data").get("id").asLong();
    }

    private int countVouchers(Long batchId) {
        return testFixture.jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM vouchers WHERE batch_id = ?",
                Integer.class,
                batchId);
    }

    @Test
    void getAllVoucherBatchesSuccess() throws Exception {
        mockMvc.perform(get(BASE_URL).with(auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("Get Voucher Batches successfully!"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(existing.batchId()));
    }

    @Test
    void getVoucherBatchByIDSuccess() throws Exception {
        mockMvc.perform(get(BASE_URL + "/" + existing.batchId()).with(auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("Get Voucher Batch successfully!"))
                .andExpect(jsonPath("$.data.id").value(existing.batchId()))
                .andExpect(jsonPath("$.data.campaignId").value(existing.campaignId()))
                .andExpect(jsonPath("$.data.batchType").value("GENERIC"))
                .andExpect(jsonPath("$.data.durationUnit").value("DAYS"))
                .andExpect(jsonPath("$.data.durationValue").value(7))
                .andExpect(jsonPath("$.data.status").value("GENERATED"));
    }

    @Test
    void createVoucherBatchSuccess() throws Exception {
        String content = mockMvc.perform(
                        post(BASE_URL)
                                .with(auth())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(createBody(BatchType.BULK)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.message").value("Create Voucher Batch successfully!"))
                .andExpect(jsonPath("$.data.campaignId").value(existing.campaignId()))
                .andExpect(jsonPath("$.data.ruleId").value(ruleId))
                .andExpect(jsonPath("$.data.batchType").value("BULK"))
                .andExpect(jsonPath("$.data.durationValue").value(30))
                .andExpect(jsonPath("$.data.generatedCount").value(0))
                .andExpect(jsonPath("$.data.redeemedCount").value(0))
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long newId = objectMapper.readTree(content).get("data").get("id").asLong();

        // Mới tạo (DRAFT) thì chưa có voucher nào
        assertEquals(0, countVouchers(newId));
    }

    @Test
    @Transactional
    void updateVoucherBatchStatusDraftToGeneratedSuccess() throws Exception {
        Long id = createDraftBatch(BatchType.BULK);

        mockMvc.perform(
                        patch(BASE_URL + "/" + id)
                                .with(auth())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"status": "GENERATED"}
                                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("Update Voucher Batch successfully!"))
                .andExpect(jsonPath("$.data.status").value("GENERATED"))
                .andExpect(jsonPath("$.data.generatedCount").value(5));

        // BULK: mỗi quota sinh 1 mã -> 3 + 2 = 5 voucher
        assertEquals(5, countVouchers(id));
        VoucherBatch batch = voucherBatchRepository.findById(id).orElseThrow();

        assertEquals("PUBLISHED", batch.getRule().getCondition().getStatus().name());
        assertEquals("PUBLISHED", batch.getRule().getReward().getStatus().name());
    }

    @Test
    void updateVoucherBatchStatusGeneratedFailed() throws Exception {
        Long id = createDraftBatch(BatchType.BULK);

        // Lần 1: DRAFT -> GENERATED thành công
        mockMvc.perform(
                        patch(BASE_URL + "/" + id)
                                .with(auth())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"status": "GENERATED"}
                                        """))
                .andExpect(status().isOk());

        // Lần 2: batch đã GENERATED nên không được sinh/sửa nữa
        // TODO: đổi sang đúng status code mà VoucherBatchTypeException được map (400/409...)
        mockMvc.perform(
                        patch(BASE_URL + "/" + id)
                                .with(auth())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"status": "GENERATED"}
                                        """))
                .andExpect(status().isBadRequest());

        // Không sinh thêm voucher
        assertEquals(5, countVouchers(id));
    }
}