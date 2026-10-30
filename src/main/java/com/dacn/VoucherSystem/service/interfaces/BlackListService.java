package com.dacn.VoucherSystem.service.interfaces;

import com.dacn.VoucherSystem.dto.response.BlackListResponse;
import java.util.List;

public interface BlackListService {
    List<BlackListResponse> getAllBlackList();
    BlackListResponse getBlackListById(Long id);
}
