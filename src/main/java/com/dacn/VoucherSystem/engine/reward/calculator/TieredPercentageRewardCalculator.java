package com.dacn.VoucherSystem.engine.reward.calculator;

import com.dacn.VoucherSystem.engine.context.RuleEvaluationContext;
import com.dacn.VoucherSystem.engine.result.RewardResult;
import com.dacn.VoucherSystem.engine.reward.RewardCalculator;
import com.dacn.VoucherSystem.engine.reward.RewardMath;
import com.dacn.VoucherSystem.enums.RewardType;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

import java.math.BigDecimal;

import static com.dacn.VoucherSystem.util.JsonValidator.*;

@Component
public class TieredPercentageRewardCalculator implements RewardCalculator {
    @Override
    public RewardType supportedType() {
        return RewardType.TIERED_PERCENTAGE;
    }

    @Override
    public RewardResult calculate(JsonNode config, RuleEvaluationContext context) {
        JsonNode tiers = requiredArray(config, "tiers", "config");
        JsonNode matched = null;
        // Validator guarantees ascending minCartAmount and HIGHEST_MATCH configuration.
        for (JsonNode tier : tiers) {
            BigDecimal minimum = requiredNumber(tier, "minCartAmount", "config.tiers[]");
            if (context.cartSubtotal().compareTo(minimum) >= 0) matched = tier;
            else break;
        }
        if (matched == null) return RewardResult.cartDiscount(BigDecimal.ZERO.setScale(2));

        BigDecimal percent = requiredNumber(matched, "discountPercent", "config.tiers[]");
        BigDecimal max = requiredNumber(matched, "maxDiscountAmount", "config.tiers[]");
        BigDecimal calculated = RewardMath.percentage(context.cartSubtotal(), percent);
        BigDecimal discount = RewardMath.money(RewardMath.min(
                context.cartSubtotal(), RewardMath.min(calculated, max)));
        return RewardResult.cartDiscount(discount);
    }
}
