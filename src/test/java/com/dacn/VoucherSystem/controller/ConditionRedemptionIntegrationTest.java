package com.dacn.VoucherSystem.controller;

import com.dacn.VoucherSystem.BaseIntegrationTest;
import com.dacn.VoucherSystem.enums.*;
import com.dacn.VoucherSystem.fixture.RedemptionTestFixture;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class ConditionRedemptionIntegrationTest extends BaseIntegrationTest {

    private static final String MIN_50 = "{\"minimumAmount\": 50}";
    private static final String VIP_IN = "{\"operator\":\"IN\",\"segments\":[\"VIP\"]}";
    private static final String VIP_NOT_IN = "{\"operator\":\"NOT_IN\",\"segments\":[\"VIP\"]}";

    @Autowired
    private RedemptionTestFixture testFixture;

    @BeforeEach
    void setUp() {
        testFixture.cleanDatabase();
    }

    @AfterEach
    void tearDown() {
        testFixture.cleanDatabase();
    }

    @Test
    void minCartAmountBoundaryIsInclusive() throws Exception {
        RedemptionTestFixture.Fixture f = createWith(ConditionType.MIN_CART_AMOUNT, MIN_50, RuleStatus.PUBLISHED);

        redeem(f.code(), "C-BELOW", "49.99", Set.of())
                .andExpect(status().isUnprocessableEntity());
        assertEquals(1, countLogs(f.voucherId(), "C-BELOW", "FAILED"));

        redeem(f.code(), "C-EXACT", "50.00", Set.of())
                .andExpect(status().isOk());
    }

    @Test
    void timeWindowAllowsRedemptionInsideWindow() throws Exception {
        LocalTime nowUtc = LocalTime.now(ZoneId.of("UTC")).withSecond(0).withNano(0);
        String config = timeWindowConfig(nowUtc.minusHours(1), nowUtc.plusHours(1));
        RedemptionTestFixture.Fixture f = createWith(ConditionType.TIME_WINDOW, config, RuleStatus.PUBLISHED);

        redeem(f.code(), "C-IN-WINDOW", "100.00", Set.of())
                .andExpect(status().isOk());
    }

    @Test
    void timeWindowRejectsRedemptionOutsideWindow() throws Exception {
        LocalTime nowUtc = LocalTime.now(ZoneId.of("UTC")).withSecond(0).withNano(0);
        String config = timeWindowConfig(nowUtc.plusHours(2), nowUtc.plusHours(3));
        RedemptionTestFixture.Fixture f = createWith(ConditionType.TIME_WINDOW, config, RuleStatus.PUBLISHED);

        redeem(f.code(), "C-OUT-WINDOW", "100.00", Set.of())
                .andExpect(status().isUnprocessableEntity());
        assertEquals(1, countLogs(f.voucherId(), "C-OUT-WINDOW", "FAILED"));
    }

    @Test
    void customerSegmentInRequiresMatchingSegment() throws Exception {
        RedemptionTestFixture.Fixture f = createWith(ConditionType.CUSTOMER_SEGMENT, VIP_IN, RuleStatus.PUBLISHED);

        redeem(f.code(), "C-VIP", "100.00", Set.of("VIP"))
                .andExpect(status().isOk());
        redeem(f.code(), "C-NORMAL", "100.00", Set.of("NORMAL"))
                .andExpect(status().isUnprocessableEntity());
        redeem(f.code(), "C-NONE", "100.00", Set.of())
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void customerSegmentNotInExcludesMatchingSegment() throws Exception {
        RedemptionTestFixture.Fixture f = createWith(ConditionType.CUSTOMER_SEGMENT, VIP_NOT_IN, RuleStatus.PUBLISHED);

        redeem(f.code(), "C-VIP-EXCLUDED", "100.00", Set.of("VIP"))
                .andExpect(status().isUnprocessableEntity());
        redeem(f.code(), "C-NORMAL-OK", "100.00", Set.of("NORMAL"))
                .andExpect(status().isOk());
    }

    @Test
    void compositeAndRequiresAllChildren() throws Exception {
        String config = """
                {"operator":"AND","conditions":[
                  {"type":"MIN_CART_AMOUNT","config":{"minimumAmount":50}},
                  {"type":"CUSTOMER_SEGMENT","config":{"operator":"IN","segments":["VIP"]}}
                ]}
                """;
        RedemptionTestFixture.Fixture f = createWith(ConditionType.COMPOSITE, config, RuleStatus.PUBLISHED);

        redeem(f.code(), "C-AND-OK", "60.00", Set.of("VIP"))
                .andExpect(status().isOk());
        redeem(f.code(), "C-AND-NO-SEGMENT", "60.00", Set.of("NORMAL"))
                .andExpect(status().isUnprocessableEntity());
        redeem(f.code(), "C-AND-LOW-CART", "40.00", Set.of("VIP"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void compositeOrAcceptsAnyChild() throws Exception {
        String config = """
                {"operator":"OR","conditions":[
                  {"type":"MIN_CART_AMOUNT","config":{"minimumAmount":50}},
                  {"type":"CUSTOMER_SEGMENT","config":{"operator":"IN","segments":["VIP"]}}
                ]}
                """;
        RedemptionTestFixture.Fixture f = createWith(ConditionType.COMPOSITE, config, RuleStatus.PUBLISHED);

        redeem(f.code(), "C-OR-VIP-LOW", "40.00", Set.of("VIP"))
                .andExpect(status().isOk());
        redeem(f.code(), "C-OR-NONE-LOW", "40.00", Set.of("NORMAL"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void unpublishedConditionCannotBeRedeemed() throws Exception {
        RedemptionTestFixture.Fixture f = createWith(ConditionType.MIN_CART_AMOUNT, MIN_50, RuleStatus.DRAFT);

        redeem(f.code(), "C-DRAFT", "100.00", Set.of())
                .andExpect(status().isUnprocessableEntity());
        assertEquals(1, countLogs(f.voucherId(), "C-DRAFT", "FAILED"));
    }

    // ---------- helpers ----------

    private RedemptionTestFixture.Fixture createWith(
            ConditionType type,
            String config,
            RuleStatus conditionStatus
    ) throws Exception {
        return testFixture.createFixture(
                BatchType.GENERIC,
                CampaignStatus.ACTIVE,
                1,
                100,
                type,
                objectMapper.readTree(config),
                conditionStatus,
                RewardType.PERCENTAGE,
                objectMapper.readTree("{\"discountPercent\": 10, \"maxDiscountAmount\": 50, \"amountBasis\": \"CART_SUBTOTAL\"}")
        );
    }

    private String timeWindowConfig(LocalTime start, LocalTime end) {
        return """
                {"timezone":"UTC","windows":[{"startTime":"%s","endTime":"%s"}]}
                """.formatted(start, end);
    }

    private ResultActions redeem(String code, String customerId, String subtotal, Set<String> segments)
            throws Exception {
        BigDecimal amount = new BigDecimal(subtotal);
        Map<String, Object> body = Map.of(
                "voucherCode", code,
                "customerId", customerId,
                "cartSubtotal", amount,
                "customerSegments", segments,
                "items", List.of(Map.of(
                        "productId", "PRODUCT-01",
                        "quantity", 1,
                        "unitPrice", amount
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