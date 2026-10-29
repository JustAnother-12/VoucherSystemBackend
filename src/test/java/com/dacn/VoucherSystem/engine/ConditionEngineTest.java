package com.dacn.VoucherSystem.engine;

import com.dacn.VoucherSystem.engine.condition.ConditionEngine;
import com.dacn.VoucherSystem.engine.condition.ConditionEvaluator;
import com.dacn.VoucherSystem.engine.condition.evaluator.CustomerSegmentEvaluator;
import com.dacn.VoucherSystem.engine.condition.evaluator.MinCartAmountEvaluator;
import com.dacn.VoucherSystem.engine.condition.evaluator.TimeWindowEvaluator;
import com.dacn.VoucherSystem.engine.context.RuleEvaluationContext;
import com.dacn.VoucherSystem.enums.ConditionType;
import com.dacn.VoucherSystem.exception.InvalidRuleConfigException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ConditionEngineTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private ConditionEngine engine;

    @BeforeEach
    void setUp() {
        List<ConditionEvaluator> evaluators = List.of(
                new MinCartAmountEvaluator(),
                new CustomerSegmentEvaluator(),
                new TimeWindowEvaluator());
        engine = new ConditionEngine(evaluators);
    }

    private JsonNode json(String value) {
        try {
            return mapper.readTree(value);
        } catch (Exception ex) {
            throw new AssertionError("Invalid JSON in test", ex);
        }
    }
    private RuleEvaluationContext context(String subtotal, Set<String> segments, String instant) {
        return new RuleEvaluationContext(
                new BigDecimal(subtotal), segments,
                Instant.parse(instant), List.of()
        );
    }

    @Test
    void minimumCartAmount_returnsTrueAtBoundary() {
        assertTrue(engine.evaluate(ConditionType.MIN_CART_AMOUNT,
                json(
                        """
                        {"minimumAmount":200000,"amountBasis":"CART_SUBTOTAL"}
                        """
                ),
                context("200000", Set.of(), String.valueOf(Instant.now()))));
    }

    @Test
    void minimumCartAmount_returnsTrueAboveBoundary() {
        assertTrue(engine.evaluate(ConditionType.MIN_CART_AMOUNT,
                json(
                        """
                        {"minimumAmount":200000,"amountBasis":"CART_SUBTOTAL"}
                        """
                ),
                context("250000", Set.of(), String.valueOf(Instant.now()))));
    }

    @Test
    void minimumCartAmount_returnsFalseBelowBoundary() {
        assertFalse(engine.evaluate(ConditionType.MIN_CART_AMOUNT,
                json(
                        """
                        {"minimumAmount":200000,"amountBasis":"CART_SUBTOTAL"}
                        """
                ),
                context("199999", Set.of(), String.valueOf(Instant.now()))));
    }

    @Test
    void customerSegment_supportsInAndNotIn() {
        var ctx = context("100000", Set.of("VIP"), String.valueOf(Instant.now()));
        assertTrue(engine.evaluate(ConditionType.CUSTOMER_SEGMENT,
                json("""
                        {"operator":"IN","segments":["VIP"]}
                        """), ctx));
        assertFalse(engine.evaluate(ConditionType.CUSTOMER_SEGMENT,
                json(
                        """
                        {"operator":"NOT_IN","segments":["VIP"]}
                        """
                ), ctx));
    }

    @Test
    void timeWindow_usesConfiguredTimezoneAndStartInclusiveEndExclusive() {
        var ctx = context("100000", Set.of(), "2026-10-29T05:30:00Z"); // 12:30 in Vietnam
        assertTrue(engine.evaluate(ConditionType.TIME_WINDOW,
                json(
                        """
                        {"timezone":"Asia/Ho_Chi_Minh","windows":[{"startTime":"12:00:00","endTime":"13:00:00"}]}
                        """
                ), ctx));
    }

    @Test
    void timeWindow_supportsMidnightCrossingWindow() {
        var ctx = context("100000", Set.of(), "2026-10-29T17:00:00Z"); // 00:00 next day in Vietnam
        assertTrue(engine.evaluate(ConditionType.TIME_WINDOW,
                json(
                        """
                        {"timezone":"Asia/Ho_Chi_Minh","windows":[{"startTime":"22:00:00","endTime":"02:00:00"}]}
                        """
                ), ctx));
    }

    @Test
    void timeWindow_declinesEqualsStartAndEndTime() {
        var ctx = context("100000", Set.of(), "2026-10-29T17:00:00Z"); // 00:00 next day in Vietnam
        assertThrows(
                InvalidRuleConfigException.class,
                () -> engine.evaluate(
                        ConditionType.TIME_WINDOW,
                        json(
                        """
                        {"timezone":"Asia/Ho_Chi_Minh","windows":[{"startTime":"20:00:00","endTime":"20:00:00"}]}
                        """),
                        ctx)
        );
    }

    @Test
    void compositeAnd_requiresEveryChildToMatch() {
        var ctx = context("300000", Set.of("VIP"), "2026-10-29T05:30:00Z");
        var config = json(
        """
        {
          "operator":"AND","conditions":
          [
            {"type":"CUSTOMER_SEGMENT","config":{"operator":"IN","segments":["VIP"]}},
            {"type":"MIN_CART_AMOUNT","config":{"minimumAmount":200000,"amountBasis":"CART_SUBTOTAL"}}
          ]
        }
        """);
        assertTrue(engine.evaluate(ConditionType.COMPOSITE, config, ctx));
    }

    @Test
    void compositeOr_passesWhenAnyChildMatches() {
        var ctx = context("100000", Set.of("NORMAL"), "2026-10-29T05:30:00Z");
        var config = json(
        """
        {
          "operator":"OR","conditions":
          [
            {"type":"CUSTOMER_SEGMENT","config":{"operator":"IN","segments":["VIP"]}},
            {"type":"MIN_CART_AMOUNT","config":{"minimumAmount":50000,"amountBasis":"CART_SUBTOTAL"}}
          ]
        }
        """);
        // The segment does not match, but the minimum cart amount does.
        assertTrue(engine.evaluate(ConditionType.COMPOSITE, config, ctx));
    }
}
