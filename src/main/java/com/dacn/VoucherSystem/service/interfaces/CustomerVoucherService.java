package com.dacn.VoucherSystem.service.interfaces;

import com.dacn.VoucherSystem.dto.response.CustomerVoucherResponse;
import com.dacn.VoucherSystem.enums.CustomerVoucherStatus;

import java.util.List;

public interface CustomerVoucherService {
    List<CustomerVoucherResponse> getAllCustomerVouchers(String customerId, CustomerVoucherStatus status);
    CustomerVoucherResponse getCustomerVoucherById(Long id);
    CustomerVoucherResponse createCustomerVoucher(String customerId, Long voucherId);
    CustomerVoucherResponse updateCustomerVoucherStatus(Long id, CustomerVoucherStatus status);
    void deleteCustomerVoucher(Long id);
}