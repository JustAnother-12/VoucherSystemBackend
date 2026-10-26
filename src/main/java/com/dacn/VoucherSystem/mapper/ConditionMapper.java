package com.dacn.VoucherSystem.mapper;

import com.dacn.VoucherSystem.dto.response.ConditionResponse;
import com.dacn.VoucherSystem.entity.Condition;
import org.springframework.stereotype.Component;

@Component
public class ConditionMapper {
    public ConditionResponse toConditionResponse(Condition condition) {
        if (condition == null) {
            return null;
        }
        return ConditionResponse.builder()
                .id(condition.getId())
                .name(condition.getConditionName())
                .conditionType(condition.getConditionType())
                .status(condition.getStatus())
                .config(condition.getConditionJson())
                .build();
    }
}
