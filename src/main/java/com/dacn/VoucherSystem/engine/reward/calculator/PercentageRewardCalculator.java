package com.dacn.VoucherSystem.engine.reward.calculator;

import com.dacn.VoucherSystem.engine.context.RuleEvaluationContext;
import com.dacn.VoucherSystem.engine.result.RewardResult;
import com.dacn.VoucherSystem.engine.reward.RewardCalculator;
import com.dacn.VoucherSystem.engine.reward.RewardMath;
import com.dacn.VoucherSystem.enums.RewardType;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

import static com.dacn.VoucherSystem.util.JsonValidator.*;

@Component
public class PercentageRewardCalculator implements RewardCalculator {
    @Override
    public RewardType supportedType() {
        return RewardType.PERCENTAGE;
    }

    @Override
    public RewardResult calculate(JsonNode config, RuleEvaluationContext context) {
        var percent = requiredNumber(config, "discountPercent", "config");
        var maxDiscount = requiredNumber(config, "maxDiscountAmount", "config");
        var calculated = RewardMath.percentage(context.cartSubtotal(), percent);
        var discount = RewardMath.money(
                RewardMath.min(context.cartSubtotal(), RewardMath.min(calculated, maxDiscount))
        );
        return RewardResult.cartDiscount(discount);
    }
}
