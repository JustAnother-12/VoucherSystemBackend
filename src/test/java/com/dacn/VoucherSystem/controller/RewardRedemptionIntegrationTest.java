package com.dacn.VoucherSystem.controller;

import com.dacn.VoucherSystem.BaseIntegrationTest;
import com.dacn.VoucherSystem.enums.BatchType;
import com.dacn.VoucherSystem.enums.CampaignStatus;
import com.dacn.VoucherSystem.enums.ConditionType;
import com.dacn.VoucherSystem.enums.RewardType;
import com.dacn.VoucherSystem.enums.RuleStatus;
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
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class RewardRedemptionIntegrationTest extends BaseIntegrationTest {

    private static final String MIN_50 = "{\"minimumAmount\": 50, \"amountBasis\": \"CART_SUBTOTAL\"}";

    private static final String PCT_10_CAP_50 =
            "{\"discountPercent\":10,\"maxDiscountAmount\":50,\"amountBasis\":\"CART_SUBTOTAL\"}";

    private static final String TIERED = """
            {"calculationMode":"HIGHEST_MATCH","amountBasis":"CART_SUBTOTAL","tiers":[
              {"minCartAmount":50,"discountPercent":10,"maxDiscountAmount":20},
              {"minCartAmount":200,"discountPercent":20,"maxDiscountAmount":100}
            ]}
            """;

    private static final String BUY_X_GET_Y = """
            {"calculationMode":"PER_QUALIFYING_SET",
             "buy":{"productIds":["A"],"quantity":2},
             "get":{"productIds":["B"],"quantity":1,"discountPercent":50},
             "maxApplicationsPerOrder":2}
            """;

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
    void percentageReward_appliesRateAndCap() throws Exception {
        RedemptionTestFixture.Fixture f = fixtureWith(RewardType.PERCENTAGE, PCT_10_CAP_50);

        // 10% of 100 = 10
        assertEquals(0, new BigDecimal("10.00").compareTo(
                discountOf(redeem(f.code(), "C-PCT-1", new BigDecimal("100.00"),
                        List.of(item("PRODUCT-01", 1, "100.00"))))));

        // 10% of 1000 = 100, bị cap 50
        assertEquals(0, new BigDecimal("50.00").compareTo(
                discountOf(redeem(f.code(), "C-PCT-2", new BigDecimal("1000.00"),
                        List.of(item("PRODUCT-01", 1, "1000.00"))))));
    }

    @Test
    void fixedReward_isCappedAtSubtotal() throws Exception {
        RedemptionTestFixture.Fixture f = fixtureWith(RewardType.FIXED, "{\"discountValue\":100}");

        // discountValue 100 nhưng đơn 60 -> giảm tối đa 60, thanh toán 0
        ResultActions result = redeem(f.code(), "C-FIXED", new BigDecimal("60.00"),
                List.of(item("PRODUCT-01", 1, "60.00")));
        assertEquals(0, new BigDecimal("60.00").compareTo(discountOf(result)));
    }

    @Test
    void tieredReward_picksHighestMatchingTier() throws Exception {
        RedemptionTestFixture.Fixture f = fixtureWith(RewardType.TIERED_PERCENTAGE, TIERED);

        // đơn 100 khớp bậc 50: 10% = 10
        assertEquals(0, new BigDecimal("10.00").compareTo(
                discountOf(redeem(f.code(), "C-TIER-1", new BigDecimal("100.00"),
                        List.of(item("PRODUCT-01", 1, "100.00"))))));

        // đơn 300 khớp bậc 200: 20% = 60, cap 100 -> 60
        assertEquals(0, new BigDecimal("60.00").compareTo(
                discountOf(redeem(f.code(), "C-TIER-2", new BigDecimal("300.00"),
                        List.of(item("PRODUCT-01", 1, "300.00"))))));
    }

    @Test
    void buyXGetY_discountsCheapestEligibleGetUnit() throws Exception {
        RedemptionTestFixture.Fixture f = fixtureWith(RewardType.BUY_X_GET_Y, BUY_X_GET_Y);

        // A x4 (30) + B x1 (20) = 140. Mua 4 A -> 2 lần áp dụng -> giảm 1 B (50% của 20) = 10
        List<Map<String, Object>> items = List.of(
                item("A", 4, "30.00"),
                item("B", 1, "20.00"));
        assertEquals(0, new BigDecimal("10.00").compareTo(
                discountOf(redeem(f.code(), "C-BXGY-1", new BigDecimal("140.00"), items))));
    }

    @Test
    void buyXGetY_withoutEnoughBuyQuantity_givesZeroDiscount() throws Exception {
        RedemptionTestFixture.Fixture f = fixtureWith(RewardType.BUY_X_GET_Y, BUY_X_GET_Y);

        // A x1 không đủ điều kiện (cần 2) -> không giảm
        List<Map<String, Object>> items = List.of(
                item("A", 1, "30.00"),
                item("B", 1, "20.00"));
        assertEquals(0, BigDecimal.ZERO.compareTo(
                discountOf(redeem(f.code(), "C-BXGY-0", new BigDecimal("50.00"), items))));
    }

    // ---------- helpers ----------

    private RedemptionTestFixture.Fixture fixtureWith(RewardType type, String rewardJson) throws Exception {
        return testFixture.createFixture(
                BatchType.GENERIC,
                CampaignStatus.ACTIVE,
                1,
                100,
                ConditionType.MIN_CART_AMOUNT,
                objectMapper.readTree(MIN_50),
                RuleStatus.PUBLISHED,
                type,
                objectMapper.readTree(rewardJson)
        );
    }

    private BigDecimal discountOf(ResultActions result) throws Exception {
        String json = result.andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(json).path("data").path("discountAmount").decimalValue();
    }

    private ResultActions redeem(String code, String customerId, BigDecimal subtotal,
                                 List<Map<String, Object>> items) throws Exception {
        Map<String, Object> body = Map.of(
                "voucherCode", code,
                "customerId", customerId,
                "cartSubtotal", subtotal,
                "customerSegments", Set.of(),
                "items", items
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

    private static Map<String, Object> item(String productId, int quantity, String unitPrice) {
        return Map.of(
                "productId", productId,
                "quantity", quantity,
                "unitPrice", new BigDecimal(unitPrice)
        );
    }
}