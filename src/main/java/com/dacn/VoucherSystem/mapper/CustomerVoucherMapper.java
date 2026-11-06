package com.dacn.VoucherSystem.mapper;

import com.dacn.VoucherSystem.dto.response.CustomerVoucherResponse;
import com.dacn.VoucherSystem.entity.CustomerVoucher;
import org.springframework.stereotype.Component;

@Component
public class CustomerVoucherMapper {
    public CustomerVoucherResponse toCustomerVoucherResponse(CustomerVoucher customerVoucher) {
        return CustomerVoucherResponse.builder()
                .id(customerVoucher.getId())
                .customerId(customerVoucher.getCustomerId())
                .voucherId(customerVoucher.getVoucher().getId())
                .voucherCode(customerVoucher.getVoucher().getCode())
                .status(customerVoucher.getStatus())
                .claimedAt(customerVoucher.getClaimedAt())
                .expiredAt(customerVoucher.getExpiredAt())
                .build();
    }
}