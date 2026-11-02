package com.dacn.VoucherSystem.service.interfaces;

import com.dacn.VoucherSystem.dto.response.BlackListResponse;
import com.dacn.VoucherSystem.enums.BlacklistType;
import java.time.LocalDateTime;
import java.util.List;

public interface BlackListService {
    List<BlackListResponse> getAllBlackList();
    BlackListResponse getBlackListById(Long id);
    BlackListResponse createBlackList(
            BlacklistType blacklistType,
            String targetValue,
            String reason,
            LocalDateTime expiredAt
    );

    BlackListResponse deactivateBlackList(Long id);
}
