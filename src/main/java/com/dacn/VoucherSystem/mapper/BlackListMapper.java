package com.dacn.VoucherSystem.mapper;

import com.dacn.VoucherSystem.dto.response.BlackListResponse;
import com.dacn.VoucherSystem.entity.BlackList;
import org.springframework.stereotype.Component;

@Component
public class BlackListMapper {
    public BlackListResponse toBlackListResponse(BlackList blackList) {
        if (blackList == null) {
            return null;
        }
        return BlackListResponse.builder()
                .id(blackList.getId())
                .type(String.valueOf(blackList.getBlacklistType()))
                .value(blackList.getTargetValue())
                .isActive(blackList.getIsActive())
                .reason(blackList.getReason())
                .expiredAt(blackList.getExpiredAt())
                .createAt(blackList.getCreatedAt())
                .build();
    }
}
