package com.dacn.VoucherSystem.mapper;

import com.dacn.VoucherSystem.dto.response.VoucherResponse;
import com.dacn.VoucherSystem.entity.Voucher;
import org.springframework.stereotype.Component;

@Component
public class VoucherMapper {
    public VoucherResponse toVoucherResponse(Voucher voucher) {
        return VoucherResponse.builder()
                .id(voucher.getId())
                .code(voucher.getCode())
                .batchId(voucher.getBatch().getId())
                .campaignId(voucher.getCampaign().getId())
                .createdAt(voucher.getCreatedAt())
                .build();
    }
}