package com.dacn.VoucherSystem.service.interfaces;

import com.dacn.VoucherSystem.dto.response.VoucherResponse;

import java.util.List;

public interface VoucherService {
    List<VoucherResponse> getAllVouchers(Long batchId, Long campaignId);
    VoucherResponse getVoucherById(Long id);
    VoucherResponse getVoucherByCode(String code);
}