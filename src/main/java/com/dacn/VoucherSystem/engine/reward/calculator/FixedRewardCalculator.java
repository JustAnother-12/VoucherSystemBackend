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
public class FixedRewardCalculator implements RewardCalculator {
    @Override
    public RewardType supportedType() {
        return RewardType.FIXED;
    }

    @Override
    public RewardResult calculate(JsonNode config, RuleEvaluationContext context) {
        var configured = requiredNumber(config, "discountValue", "config");
        var discount = RewardMath.money(
                RewardMath.min(configured, context.cartSubtotal())
        );
        return RewardResult.cartDiscount(discount);
    }
}
