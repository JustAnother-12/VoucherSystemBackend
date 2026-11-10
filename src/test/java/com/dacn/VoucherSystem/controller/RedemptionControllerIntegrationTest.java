package com.dacn.VoucherSystem.controller;

import com.dacn.VoucherSystem.BaseIntegrationTest;
import com.dacn.VoucherSystem.dto.request.VoucherRedeemRequest;
import com.dacn.VoucherSystem.entity.*;
import com.dacn.VoucherSystem.enums.*;
import com.dacn.VoucherSystem.fixture.RedemptionTestFixture;
import com.dacn.VoucherSystem.repository.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class RedemptionControllerIntegrationTest extends BaseIntegrationTest {
    @Autowired
    private RedemptionTestFixture testFixture;

    @Autowired
    private VoucherRepository voucherRepository;

    @Autowired
    private CustomerVoucherRepository customerVoucherRepository;

    private RedemptionTestFixture.Fixture genericFixture;

    @BeforeEach
    void setUp() throws Exception {
        testFixture.cleanDatabase();

        genericFixture = testFixture.createFixture(
                BatchType.GENERIC,
                CampaignStatus.ACTIVE
        );
    }

    @AfterEach
    void tearDown() {
        testFixture.cleanDatabase();
    }

    private VoucherRedeemRequest createRequest(
            String code,
            String customerId,
            BigDecimal subtotal
    ) {
        return new VoucherRedeemRequest(
                code,
                customerId,
                subtotal,
                Set.of(),
                List.of(
                        new VoucherRedeemRequest.CartItemRequest(
                                "PRODUCT-01",
                                1,
                                subtotal
                        )
                )
        );
    }

    private ResultActions redeem(
            String code,
            String customerId,
            BigDecimal subtotal
    ) throws Exception {

        VoucherRedeemRequest request = createRequest(
                code,
                customerId,
                subtotal
        );

        return mockMvc.perform(
                post("/redemptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(
                                "X-Device-Fingerprint",
                                "TEST-DEVICE-001"
                        )
                        .content(objectMapper.writeValueAsString(request))
                        .with(mockRequest -> {
                            mockRequest.setRemoteAddr("127.0.0.1");
                            return mockRequest;
                        })
        );
    }

    private int redeemAndGetStatus(
            String code,
            String customerId,
            BigDecimal subtotal
    ) throws Exception {
        return redeem(code, customerId, subtotal).andReturn().getResponse().getStatus();
    }

    private int getRedeemedCount(Long batchId) {
        return testFixture.jdbcTemplate.queryForObject(
                """
                SELECT redeemed_count
                FROM voucher_batches
                WHERE id = ?
                """,
                Integer.class,
                batchId
        );
    }

    private int countLogs(
            Long voucherId,
            String customerId,
            String status
    ) {
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

    @Test
    void redeemGenericVoucherSuccessfully() throws Exception {
        String customerId = "CUSTOMER-001";
        redeem(
                genericFixture.code(),
                customerId,
                new BigDecimal("100.00")
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message")
                        .value("Voucher applied successfully"))
                .andExpect(jsonPath("$.data.voucherId")
                        .value(genericFixture.voucherId()))
                .andExpect(jsonPath("$.data.originalAmount")
                        .value(100.00))
                .andExpect(jsonPath("$.data.discountAmount")
                        .value(10.00))
                .andExpect(jsonPath("$.data.finalAmount")
                        .value(90.00));

        assertEquals(
                1,
                getRedeemedCount(genericFixture.batchId())
        );

        assertEquals(
                1,
                countLogs(
                        genericFixture.voucherId(),
                        customerId,
                        "SUCCESS"
                )
        );

        assertEquals(
                0,
                countLogs(
                        genericFixture.voucherId(),
                        customerId,
                        "FAILED"
                )
        );
    }

    @Test
    void redeemUnknownVoucherShouldReturn404AndNotCreateLog()
            throws Exception {

        String unknownCode = "UNKNOWN-" + UUID.randomUUID();

        redeem(
                unknownCode,
                "CUSTOMER-001",
                new BigDecimal("100.00")
        )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));

        Integer logCount = testFixture.jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM redemption_logs",
                Integer.class
        );

        assertEquals(0, logCount);
    }

    @Test
    void redeemVoucherWhenConditionNotSatisfied()
            throws Exception {

        String customerId = "CUSTOMER-002";

        redeem(
                genericFixture.code(),
                customerId,
                new BigDecimal("20.00")
        )
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.message")
                        .value("Cart does not satisfy voucher conditions"));

        assertEquals(
                0,
                getRedeemedCount(genericFixture.batchId())
        );

        assertEquals(
                1,
                countLogs(
                        genericFixture.voucherId(),
                        customerId,
                        "FAILED"
                )
        );

        assertEquals(
                0,
                countLogs(
                        genericFixture.voucherId(),
                        customerId,
                        "SUCCESS"
                )
        );
    }

    @Test
    void redeemVoucherWhenCampaignExpired()
            throws Exception {

        LocalDateTime expiredTime =
                LocalDateTime.now(testFixture.clock).minusMinutes(1);

        testFixture.jdbcTemplate.update(
                """
                UPDATE campaigns
                SET end_time = ?
                WHERE id = ?
                """,
                expiredTime,
                genericFixture.campaignId()
        );

        String customerId = "CUSTOMER-003";

        redeem(
                genericFixture.code(),
                customerId,
                new BigDecimal("100.00")
        )
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.message")
                        .value("Campaign is inactive or expired"));

        assertEquals(
                0,
                getRedeemedCount(genericFixture.batchId())
        );

        assertEquals(
                1,
                countLogs(
                        genericFixture.voucherId(),
                        customerId,
                        "FAILED"
                )
        );
    }

    @Test
    void bulkVoucherShouldBeRejectedForUnassignedCustomer()
            throws Exception {

        RedemptionTestFixture.Fixture bulkFixture = testFixture.createFixture(
                BatchType.BULK,
                CampaignStatus.ACTIVE
        );

        String customerId = "CUSTOMER-NOT-ASSIGNED";

        redeem(
                bulkFixture.code(),
                customerId,
                new BigDecimal("100.00")
        )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message")
                        .value("Voucher is no longer available to this customer"));

        assertEquals(
                0,
                getRedeemedCount(bulkFixture.batchId())
        );

        assertEquals(
                1,
                countLogs(
                        bulkFixture.voucherId(),
                        customerId,
                        "FAILED"
                )
        );

        assertEquals(
                0,
                countLogs(
                        bulkFixture.voucherId(),
                        customerId,
                        "SUCCESS"
                )
        );
    }

    @Test
    void assignedBulkVoucherShouldBeRedeemedSuccessfully()
            throws Exception {

        RedemptionTestFixture.Fixture bulkFixture = testFixture.createFixture(
                BatchType.BULK,
                CampaignStatus.ACTIVE
        );

        String customerId = "CUSTOMER-BULK-001";

        CustomerVoucher customerVoucher = CustomerVoucher.builder()
                .claimedAt(LocalDateTime.now())
                .voucher(
                        voucherRepository.findById(bulkFixture.voucherId())
                                .orElseThrow()
                )
                .customerId(customerId)
                .status(CustomerVoucherStatus.AVAILABLE)
                .expiredAt(
                        LocalDateTime.now(testFixture.clock).plusDays(1)
                )
                .build();

        customerVoucherRepository.saveAndFlush(customerVoucher);

        redeem(
                bulkFixture.code(),
                customerId,
                new BigDecimal("100.00")
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message")
                        .value("Voucher applied successfully"))
                .andExpect(jsonPath("$.data.voucherId")
                        .value(bulkFixture.voucherId()))
                .andExpect(jsonPath("$.data.originalAmount")
                        .value(100.00))
                .andExpect(jsonPath("$.data.discountAmount")
                        .value(10.00))
                .andExpect(jsonPath("$.data.finalAmount")
                        .value(90.00));;

        assertEquals(
                1,
                getRedeemedCount(bulkFixture.batchId())
        );

        // makes sure the status got updated in CustomerVoucher
        CustomerVoucher consumed = customerVoucherRepository
                .findByVoucherIdAndCustomerId(
                        bulkFixture.voucherId(),
                        customerId
                )
                .orElseThrow();

        assertEquals(
                CustomerVoucherStatus.USED,
                consumed.getStatus()
        );
    }

    @Test
    void genericVoucherCanOnlyBeRedeemedOncePerCustomer()
            throws Exception {

        String customerId = "CUSTOMER-GENERIC-001";

        // first redeem
        redeem(
                genericFixture.code(),
                customerId,
                new BigDecimal("100.00")
        )
                .andExpect(status().isOk());

        // second redeem
        redeem(
                genericFixture.code(),
                customerId,
                new BigDecimal("100.00")
        )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message")
                        .value("Customer has already redeemed this voucher"));

        // there should only be 1 success log
        assertEquals(
                1,
                getRedeemedCount(genericFixture.batchId())
        );

        assertEquals(
                1,
                countLogs(
                        genericFixture.voucherId(),
                        customerId,
                        "SUCCESS"
                )
        );

        assertEquals(
                1,
                countLogs(
                        genericFixture.voucherId(),
                        customerId,
                        "FAILED"
                )
        );
    }

    @Test
    void concurrentRequestsFromSameCustomerShouldOnlyRedeemOnce()
            throws Exception {

        String customerId = "CUSTOMER-CONCURRENT-001";

        ExecutorService executor = Executors.newFixedThreadPool(2);

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        Callable<Integer> request = () -> {
            ready.countDown();

            if (!start.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException(
                        "Requests could not start concurrently"
                );
            }

            return redeemAndGetStatus(
                    genericFixture.code(),
                    customerId,
                    new BigDecimal("100.00")
            );
        };

        try {
            Future<Integer> first = executor.submit(request);
            Future<Integer> second = executor.submit(request);

            assertTrue(ready.await(5, TimeUnit.SECONDS));

            // makes 2 request start at the same time
            start.countDown();

            int statusA = first.get(15, TimeUnit.SECONDS);
            int statusB = second.get(15, TimeUnit.SECONDS);

            List<Integer> statuses = List.of(statusA, statusB);

            // should only be one success status
            assertEquals(
                    1,
                    statuses.stream().filter(status -> status == 200).count()
            );

            // other response should be conflict
            assertEquals(
                    1,
                    statuses.stream().filter(status -> status == 409).count()
            );

            assertEquals(
                    1,
                    getRedeemedCount(genericFixture.batchId())
            );

            assertEquals(
                    1,
                    countLogs(
                            genericFixture.voucherId(),
                            customerId,
                            "SUCCESS"
                    )
            );

            assertEquals(
                    1,
                    countLogs(
                            genericFixture.voucherId(),
                            customerId,
                            "FAILED"
                    )
            );

        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void twoCustomersCompetingForLastBatchQuotaShouldOnlyAllowOne()
            throws Exception {

        RedemptionTestFixture.Fixture fixture = testFixture.createFixture(
                BatchType.GENERIC,
                CampaignStatus.ACTIVE,
                1,
                1
        );

        ExecutorService executor = Executors.newFixedThreadPool(2);

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        Callable<Integer> customerA = () -> {
            ready.countDown();
            start.await();

            return redeemAndGetStatus(
                    fixture.code(),
                    "CUSTOMER-QUOTA-A",
                    new BigDecimal("100.00")
            );
        };

        Callable<Integer> customerB = () -> {
            ready.countDown();
            start.await();

            return redeemAndGetStatus(
                    fixture.code(),
                    "CUSTOMER-QUOTA-B",
                    new BigDecimal("100.00")
            );
        };

        try {
            Future<Integer> first = executor.submit(customerA);
            Future<Integer> second = executor.submit(customerB);

            assertTrue(ready.await(5, TimeUnit.SECONDS));
            start.countDown();

            // makes 2 request at the same time
            int statusA = first.get(15, TimeUnit.SECONDS);
            int statusB = second.get(15, TimeUnit.SECONDS);

            List<Integer> statuses = List.of(statusA, statusB);

            assertEquals(
                    1,
                    statuses.stream().filter(status -> status == 200).count()
            );

            assertEquals(
                    1,
                    statuses.stream().filter(status -> status == 409).count()
            );

            assertEquals(1, getRedeemedCount(fixture.batchId()));

            // one of the two customer should redeem successfully
            int successfulLogs =
                    countLogs(fixture.voucherId(), "CUSTOMER-QUOTA-A", "SUCCESS")
                            + countLogs(fixture.voucherId(), "CUSTOMER-QUOTA-B", "SUCCESS");

            assertEquals(1, successfulLogs);

        } finally {
            executor.shutdownNow();
        }
    }
}
