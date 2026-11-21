package com.dacn.VoucherSystem.controller;

import com.dacn.VoucherSystem.BaseIntegrationTest;
import com.dacn.VoucherSystem.enums.BatchType;
import com.dacn.VoucherSystem.enums.CampaignStatus;
import com.dacn.VoucherSystem.enums.CustomerVoucherStatus;
import com.dacn.VoucherSystem.fixture.RedemptionTestFixture;
import com.dacn.VoucherSystem.repository.CustomerVoucherRepository;
import com.dacn.VoucherSystem.service.CustomerVoucherStatusUpdater;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class CustomerVoucherControllerIntegrationTest extends BaseIntegrationTest {

    private static final String BASE_URL = "/vouchers/customer";

    @Autowired
    private RedemptionTestFixture testFixture;

    @Autowired
    private CustomerVoucherRepository customerVoucherRepository;

    @Autowired
    private CustomerVoucherStatusUpdater customerVoucherStatusUpdater;

    @BeforeEach
    void setUp() {
        testFixture.cleanDatabase();
    }

    @AfterEach
    void tearDown() {
        testFixture.cleanDatabase();
    }

    @Test
    void claimCreatesAvailableWalletEntryWithBatchDuration() throws Exception {
        RedemptionTestFixture.Fixture bulk = createBulk();

        String response = claim("C-CLAIM", bulk.voucherId())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("AVAILABLE"))
                .andExpect(jsonPath("$.data.voucherCode").value(bulk.code()))
                .andReturn()
                .getResponse()
                .getContentAsString();

        LocalDateTime claimed = LocalDateTime.parse(objectMapper.readTree(response).path("data").path("claimedAt").asString());
        LocalDateTime expired = LocalDateTime.parse(objectMapper.readTree(response).path("data").path("expiredAt").asString());
        assertEquals(Duration.ofDays(7), Duration.between(claimed, expired));
    }

    @Test
    void claimingSameVoucherTwiceReturnsConflict() throws Exception {
        RedemptionTestFixture.Fixture bulk = createBulk();

        claim("C-TWICE", bulk.voucherId()).andExpect(status().isCreated());
        claim("C-TWICE", bulk.voucherId()).andExpect(status().isConflict());
    }

    @Test
    void claimUnknownVoucherReturnsNotFound() throws Exception {
        claim("C-UNKNOWN", 999999L).andExpect(status().isNotFound());
    }

    @Test
    void listWalletFiltersByCustomerAndStatus() throws Exception {
        RedemptionTestFixture.Fixture bulk = createBulk();
        claim("C-LIST", bulk.voucherId()).andExpect(status().isCreated());

        mockMvc.perform(get(BASE_URL).param("customerId", "C-LIST"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));

        mockMvc.perform(get(BASE_URL)
                        .param("customerId", "C-LIST")
                        .param("status", "USED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void getWalletEntryByIdUnknownReturnsNotFound() throws Exception {
        mockMvc.perform(get(BASE_URL + "/999999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void usedBulkVoucherCannotBeRedeemedUntilReactivated() throws Exception {
        RedemptionTestFixture.Fixture bulk = createBulk();
        String response = claim("C-STATUS", bulk.voucherId())
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        long walletId = objectMapper.readTree(response).path("data").path("id").asLong();

        updateStatus(walletId, "USED").andExpect(status().isOk());
        redeem(bulk.code(), "C-STATUS")
                .andExpect(status().isConflict());
        assertEquals(1, countLogs(bulk.voucherId(), "C-STATUS", "FAILED"));

        updateStatus(walletId, "AVAILABLE").andExpect(status().isOk());
        redeem(bulk.code(), "C-STATUS")
                .andExpect(status().isOk());
    }

    @Test
    void expiredBulkWalletEntryCannotBeRedeemed() throws Exception {
        RedemptionTestFixture.Fixture bulk = createBulk();
        claim("C-EXPIRED", bulk.voucherId()).andExpect(status().isCreated());

        testFixture.jdbcTemplate.update(
                "UPDATE customer_vouchers SET expired_at = ? WHERE customer_id = ?",
                Timestamp.valueOf(LocalDateTime.now(testFixture.clock).minusDays(1)),
                "C-EXPIRED"
        );

        redeem(bulk.code(), "C-EXPIRED")
                .andExpect(status().isConflict());
        assertEquals(1, countLogs(bulk.voucherId(), "C-EXPIRED", "FAILED"));
    }

    @Test
    void expirationJobExpiresAvailableAndUnavailableEntriesPastExpiredAt() throws Exception {
        RedemptionTestFixture.Fixture bulk = createBulk();
        long expiredId = walletId(claim("C-EXPIRED-JOB", bulk.voucherId())
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString());
        long unavailableId = walletId(claim("C-UNAVAILABLE-JOB", bulk.voucherId())
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString());
        long futureId = walletId(claim("C-FUTURE-JOB", bulk.voucherId())
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString());
        long usedId = walletId(claim("C-USED-JOB", bulk.voucherId())
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString());

        LocalDateTime now = LocalDateTime.now(testFixture.clock);
        testFixture.jdbcTemplate.update(
                "UPDATE customer_vouchers SET expired_at = ? WHERE id = ?",
                Timestamp.valueOf(now.minusSeconds(1)),
                expiredId
        );
        testFixture.jdbcTemplate.update(
                "UPDATE customer_vouchers SET expired_at = ? WHERE id = ?",
                Timestamp.valueOf(now.minusSeconds(1)),
                unavailableId
        );
        testFixture.jdbcTemplate.update(
                "UPDATE customer_vouchers SET expired_at = ? WHERE id = ?",
                Timestamp.valueOf(now.plusDays(1)),
                futureId
        );
        testFixture.jdbcTemplate.update(
                "UPDATE customer_vouchers SET expired_at = ? WHERE id = ?",
                Timestamp.valueOf(now.minusSeconds(1)),
                usedId
        );
        updateStatus(unavailableId, "UNAVAILABLE").andExpect(status().isOk());
        updateStatus(usedId, "USED").andExpect(status().isOk());

        customerVoucherStatusUpdater.expireCustomerVouchers();

        assertEquals(
                CustomerVoucherStatus.EXPIRED,
                customerVoucherRepository.findById(expiredId).orElseThrow().getStatus()
        );
        assertEquals(
                CustomerVoucherStatus.EXPIRED,
                customerVoucherRepository.findById(unavailableId).orElseThrow().getStatus()
        );
        assertEquals(
                CustomerVoucherStatus.AVAILABLE,
                customerVoucherRepository.findById(futureId).orElseThrow().getStatus()
        );
        assertEquals(
                CustomerVoucherStatus.USED,
                customerVoucherRepository.findById(usedId).orElseThrow().getStatus()
        );
    }

    @Test
    void deletedWalletEntryCannotBeRedeemed() throws Exception {
        RedemptionTestFixture.Fixture bulk = createBulk();
        String response = claim("C-DELETE", bulk.voucherId())
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        long walletId = objectMapper.readTree(response).path("data").path("id").asLong();

        mockMvc.perform(delete(BASE_URL + "/" + walletId))
                .andExpect(status().isOk());

        redeem(bulk.code(), "C-DELETE")
                .andExpect(status().isConflict());
    }

    @Test
    void bulkVoucherWithoutWalletEntryCannotBeRedeemed() throws Exception {
        RedemptionTestFixture.Fixture bulk = createBulk();

        redeem(bulk.code(), "C-NO-WALLET")
                .andExpect(status().isConflict());
        assertEquals(1, countLogs(bulk.voucherId(), "C-NO-WALLET", "FAILED"));
    }

    @Test
    void claimGenericVoucherIsRejected() throws Exception {
        RedemptionTestFixture.Fixture generic = testFixture.createFixture(
                BatchType.GENERIC,
                CampaignStatus.ACTIVE
        );

        claim("C-GENERIC", generic.voucherId())
                .andExpect(status().is4xxClientError());
    }

    @Test
    void claimVoucherOfInactiveCampaignIsRejected() throws Exception {
        RedemptionTestFixture.Fixture ended = testFixture.createFixture(
                BatchType.BULK,
                CampaignStatus.ENDED
        );

        claim("C-ENDED", ended.voucherId())
                .andExpect(status().is4xxClientError());
    }

    // ---------- helpers ----------

    private RedemptionTestFixture.Fixture createBulk() throws Exception {
        return testFixture.createFixture(BatchType.BULK, CampaignStatus.ACTIVE);
    }

    private ResultActions claim(String customerId, Long voucherId) throws Exception {
        String body = """
                {"customerId":"%s","voucherId":%d}
                """.formatted(customerId, voucherId);

        return mockMvc.perform(post(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private ResultActions updateStatus(long walletId, String status) throws Exception {
        return mockMvc.perform(patch(BASE_URL + "/" + walletId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"" + status + "\"}"));
    }

    private long walletId(String response) throws Exception {
        return objectMapper.readTree(response).path("data").path("id").asLong();
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

    private int countLogs(Long voucherId, String customerId, String status) {
        return testFixture.jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM redemption_logs
                WHERE voucher_id = ?
                  AND customer_id = ?
                  AND status = ?
                """,
                Integer.class,
                voucherId,
                customerId,
                status
        );
    }
}