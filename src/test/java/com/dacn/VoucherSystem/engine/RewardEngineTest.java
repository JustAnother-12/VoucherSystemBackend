package com.dacn.VoucherSystem.engine;

import com.dacn.VoucherSystem.engine.context.CartItemContext;
import com.dacn.VoucherSystem.engine.context.RuleEvaluationContext;
import com.dacn.VoucherSystem.engine.reward.RewardCalculator;
import com.dacn.VoucherSystem.engine.reward.RewardEngine;
import com.dacn.VoucherSystem.engine.reward.calculator.BuyXGetYRewardCalculator;
import com.dacn.VoucherSystem.engine.reward.calculator.FixedRewardCalculator;
import com.dacn.VoucherSystem.engine.reward.calculator.PercentageRewardCalculator;
import com.dacn.VoucherSystem.engine.reward.calculator.TieredPercentageRewardCalculator;
import com.dacn.VoucherSystem.enums.RewardType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RewardEngineTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private RewardEngine engine;

    @BeforeEach
    void setUp() {
        List<RewardCalculator> calculators = List.of(
                new FixedRewardCalculator(),
                new PercentageRewardCalculator(),
                new TieredPercentageRewardCalculator(),
                new BuyXGetYRewardCalculator());
        engine = new RewardEngine(calculators);
    }

    private JsonNode json(String value) {
        try {
            return mapper.readTree(value);
        } catch (Exception ex) {
            throw new AssertionError("Invalid JSON in test", ex);
        }
    }
    private RuleEvaluationContext context(String subtotal, List<CartItemContext> items) {
        return new RuleEvaluationContext(new BigDecimal(subtotal), Set.of(),
                Instant.now(), items);
    }

    @Test
    void fixedDiscount_isCappedAtSubtotal() {
        var result = engine.calculate(RewardType.FIXED,
                json(
                        """
                        {"discountValue":50000}
                        """
                ), context("30000", List.of()));
        assertEquals(0, result.discountAmount().compareTo(new BigDecimal("30000.00")));
    }

    @Test
    void percentageDiscount_respectsMaximumDiscount() {
        var result = engine.calculate(RewardType.PERCENTAGE,
                json(
                        """
                        {"discountPercent":30,"maxDiscountAmount":100000,"amountBasis":"CART_SUBTOTAL"}
                        """
                ),
                context("500000", List.of()));
        assertEquals(0, result.discountAmount().compareTo(new BigDecimal("100000.00")));
    }

    @Test
    void tieredPercentage_usesHighestMatchingTier() {
        var result = engine.calculate(RewardType.TIERED_PERCENTAGE,
                json(
                """
                {
                  "calculationMode":"HIGHEST_MATCH",
                  "amountBasis":"CART_SUBTOTAL",
                  "tiers":
                  [
                    {"minCartAmount":200000,"discountPercent":5,"maxDiscountAmount":70000},
                    {"minCartAmount":500000,"discountPercent":10,"maxDiscountAmount":100000},
                    {"minCartAmount":1000000,"discountPercent":15,"maxDiscountAmount":200000}
                  ]
                }
                """
                ), context("750000", List.of()));
        // discount amount should match the second tier which is 10%
        assertEquals(0, result.discountAmount().compareTo(new BigDecimal("75000.00")));
    }

    @Test
    void tieredPercentage_returnsZeroBelowFirstTier() {
        var result = engine.calculate(RewardType.TIERED_PERCENTAGE,
                json(
                """
                {
                  "calculationMode":"HIGHEST_MATCH",
                  "amountBasis":"CART_SUBTOTAL",
                  "tiers":
                  [
                    {"minCartAmount":200000,"discountPercent":5,"maxDiscountAmount":70000}
                  ]
                }
                """
                ), context("100000", List.of()));
        assertEquals(0, result.discountAmount().compareTo(BigDecimal.ZERO));
    }

    @Test
    void buyXGetY_discountsEligibleGetProductOnly() {
        var items = List.of(
                new CartItemContext("PROD-NOTEBOOK-01", 1, new BigDecimal("100000")),
                new CartItemContext("PROD-PEN-01", 2, new BigDecimal("20000")));
        var result = engine.calculate(RewardType.BUY_X_GET_Y,
                json(
                  """
                  {
                    "calculationMode":"PER_QUALIFYING_SET",
                    "buy":{"productIds":["PROD-NOTEBOOK-01"],"quantity":1},
                    "get":{"productIds":["PROD-PEN-01"],"quantity":1,"discountPercent":100},
                    "maxApplicationsPerOrder":1
                  }
                  """
                ), context("140000", items));
        assertEquals(0, result.discountAmount().compareTo(new BigDecimal("20000.00")));
        assertEquals(1, result.lineDiscounts().size());
        assertEquals("PROD-PEN-01", result.lineDiscounts().get(0).productId());
        assertEquals(1, result.lineDiscounts().get(0).quantity());
    }

    @Test
    void buyXGetY_withoutEligibleBuyProductReturnsZero() {
        var items = List.of(new CartItemContext("PROD-PEN-01", 3, new BigDecimal("20000")));
        var result = engine.calculate(RewardType.BUY_X_GET_Y,
                json(
                  """
                  {
                    "calculationMode":"PER_QUALIFYING_SET",
                    "buy":{"productIds":["PROD-NOTEBOOK-01"],"quantity":1},
                    "get":{"productIds":["PROD-PEN-01"],"quantity":1,"discountPercent":100},
                    "maxApplicationsPerOrder":1
                  }
                  """
                ), context("60000", items));
        assertEquals(0, result.discountAmount().compareTo(BigDecimal.ZERO));
    }

    @Test
    void buyXGetY_withoutEnoughBuyQuantityReturnsZero() {
        var items = List.of(
                new CartItemContext(
                        "PROD-NOTEBOOK-01", 2, new BigDecimal("100000")
                ),
                new CartItemContext(
                        "PROD-PEN-01", 1, new BigDecimal("20000")
                )
        );
        var result = engine.calculate(RewardType.BUY_X_GET_Y,
                json(
                        """
                        {
                          "calculationMode":"PER_QUALIFYING_SET",
                          "buy":{"productIds":["PROD-NOTEBOOK-01"],"quantity":3},
                          "get":{"productIds":["PROD-PEN-01"],"quantity":1,"discountPercent":100},
                          "maxApplicationsPerOrder":5
                        }
                        """
                ), context("220000", items));
        assertEquals(0, result.discountAmount().compareTo(BigDecimal.ZERO));
        assertTrue(result.lineDiscounts().isEmpty());
    }

    @Test
    void buyXGetY_discountsConfiguredGetQuantity() {
        var items = List.of(
                new CartItemContext(
                        "PROD-NOTEBOOK-01", 1, new BigDecimal("100000")
                ),
                new CartItemContext(
                        "PROD-PEN-01", 2, new BigDecimal("20000")
                )
        );
        var result = engine.calculate(RewardType.BUY_X_GET_Y,
                json(
                        """
                        {
                          "calculationMode":"PER_QUALIFYING_SET",
                          "buy":{"productIds":["PROD-NOTEBOOK-01"],"quantity":1},
                          "get":{"productIds":["PROD-PEN-01"],"quantity":2,"discountPercent":100},
                          "maxApplicationsPerOrder":1
                        }
                        """
                ), context("140000", items));
        assertEquals(
                0,
                result.discountAmount().compareTo(new BigDecimal("40000.00"))
        );
        assertEquals(1, result.lineDiscounts().size());
        assertEquals("PROD-PEN-01", result.lineDiscounts().get(0).productId());
        assertEquals(2, result.lineDiscounts().get(0).quantity());
    }

    @Test
    void buyXGetY_respectsMaxApplicationsPerOrder() {
        var items = List.of(
                new CartItemContext(
                        "PROD-NOTEBOOK-01", 5, new BigDecimal("100000")
                ),
                new CartItemContext(
                        "PROD-PEN-01", 5, new BigDecimal("20000")
                )
        );

        var result = engine.calculate(RewardType.BUY_X_GET_Y,
                json(
                        """
                        {
                          "calculationMode":"PER_QUALIFYING_SET",
                          "buy":{"productIds":["PROD-NOTEBOOK-01"],"quantity":1},
                          "get":{"productIds":["PROD-PEN-01"],"quantity":1,"discountPercent":100},
                          "maxApplicationsPerOrder":2
                        }
                        """
                ), context("600000", items));

        assertEquals(
                0,
                result.discountAmount().compareTo(new BigDecimal("40000.00"))
        );

        assertEquals(1, result.lineDiscounts().size());
        assertEquals(2, result.lineDiscounts().get(0).quantity());
    }

    @Test
    void buyXGetY_withoutEligibleGetProductReturnsZero() {
        var items = List.of(
                new CartItemContext(
                        "PROD-NOTEBOOK-01", 1, new BigDecimal("100000")
                ),
                new CartItemContext(
                        "PROD-PENCIL-01", 2, new BigDecimal("10000")
                )
        );

        var result = engine.calculate(RewardType.BUY_X_GET_Y,
                json(
                        """
                        {
                          "calculationMode":"PER_QUALIFYING_SET",
                          "buy":{"productIds":["PROD-NOTEBOOK-01"],"quantity":1},
                          "get":{"productIds":["PROD-PEN-01"],"quantity":1,"discountPercent":100},
                          "maxApplicationsPerOrder":3
                        }
                        """
                ), context("120000", items));

        assertEquals(
                0,
                result.discountAmount().compareTo(BigDecimal.ZERO)
        );
        assertTrue(result.lineDiscounts().isEmpty());
    }
}
