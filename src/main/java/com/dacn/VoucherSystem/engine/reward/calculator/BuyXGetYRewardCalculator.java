package com.dacn.VoucherSystem.engine.reward.calculator;

import com.dacn.VoucherSystem.engine.context.CartItemContext;
import com.dacn.VoucherSystem.engine.context.RuleEvaluationContext;
import com.dacn.VoucherSystem.engine.result.LineDiscount;
import com.dacn.VoucherSystem.engine.result.RewardResult;
import com.dacn.VoucherSystem.engine.reward.RewardCalculator;
import com.dacn.VoucherSystem.engine.reward.RewardMath;
import com.dacn.VoucherSystem.enums.RewardType;
import com.dacn.VoucherSystem.exception.InvalidRuleConfigException;
import com.dacn.VoucherSystem.util.RuleConfigValidator;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.util.*;

@Component
public class BuyXGetYRewardCalculator implements RewardCalculator {
    @Override
    public RewardType supportedType() {
        return RewardType.BUY_X_GET_Y;
    }

    @Override
    public RewardResult calculate(JsonNode config, RuleEvaluationContext context) {
        JsonNode buy = RuleConfigValidator.required(config, "buy", "config");
        JsonNode get = RuleConfigValidator.required(config, "get", "config");
        Set<String> buyIds = readIds(buy, "productIds", "config.buy");
        Set<String> getIds = readIds(get, "productIds", "config.get");
        int buyQuantity = RuleConfigValidator.requirePositiveInteger(buy, "quantity", "config.buy");
        int getQuantity = RuleConfigValidator.requirePositiveInteger(get, "quantity", "config.get");
        int maxApplications = RuleConfigValidator.requirePositiveInteger(config, "maxApplicationsPerOrder", "config");
        BigDecimal percent = RuleConfigValidator.requiredNumber(get, "discountPercent", "config.get");

        int eligibleBuyQuantity = context.items().stream()
                .filter(item -> buyIds.contains(item.productId()))
                .mapToInt(CartItemContext::quantity).sum();
        int applications = Math.min(eligibleBuyQuantity / buyQuantity, maxApplications);
        int unitsToDiscount = applications * getQuantity;
        if (unitsToDiscount == 0) return RewardResult.cartDiscount(BigDecimal.ZERO.setScale(2));

        // Explicit policy for this sample: discount the cheapest eligible GET units first.
        List<CartItemContext> eligibleGetItems = context.items().stream()
                .filter(item -> getIds.contains(item.productId()))
                .sorted(Comparator.comparing(CartItemContext::unitPrice))
                .toList();
        List<LineDiscount> lineDiscounts = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        int remaining = unitsToDiscount;
        for (CartItemContext item : eligibleGetItems) {
            if (remaining == 0) break;
            int allocated = Math.min(item.quantity(), remaining);
            BigDecimal lineDiscount = RewardMath.percentage(
                    item.unitPrice().multiply(BigDecimal.valueOf(allocated)), percent);
            lineDiscounts.add(new LineDiscount(item.productId(), allocated, lineDiscount));
            total = total.add(lineDiscount);
            remaining -= allocated;
        }
        total = RewardMath.money(RewardMath.min(total, context.cartSubtotal()));
        return new RewardResult(total, lineDiscounts);
    }

    private Set<String> readIds(JsonNode parent, String field, String path) {
        JsonNode ids = RuleConfigValidator.requiredArray(parent, field, path);
        Set<String> result = new HashSet<>();
        for (JsonNode id : ids) {
            if (!id.isTextual() || id.textValue().isBlank()) {
                throw new InvalidRuleConfigException(
                        path + "." + field + " must contain non-empty strings");
            }
            result.add(id.textValue());
        }
        return result;
    }
}
