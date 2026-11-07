package com.dacn.VoucherSystem.service.impl;

import com.dacn.VoucherSystem.dto.response.CustomerVoucherResponse;
import com.dacn.VoucherSystem.entity.CustomerVoucher;
import com.dacn.VoucherSystem.entity.Voucher;
import com.dacn.VoucherSystem.entity.VoucherBatch;
import com.dacn.VoucherSystem.enums.CustomerVoucherStatus;
import com.dacn.VoucherSystem.exception.ResourceAlreadyExistsException;
import com.dacn.VoucherSystem.exception.ResourceNotFoundException;
import com.dacn.VoucherSystem.mapper.CustomerVoucherMapper;
import com.dacn.VoucherSystem.repository.CustomerVoucherRepository;
import com.dacn.VoucherSystem.repository.VoucherRepository;
import com.dacn.VoucherSystem.service.interfaces.CustomerVoucherService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

import static com.dacn.VoucherSystem.util.CustomerVoucherUtil.calculateExpiredAt;

@Service
@RequiredArgsConstructor
public class CustomerVoucherServiceImpl implements CustomerVoucherService {
    private final CustomerVoucherRepository customerVoucherRepository;
    private final VoucherRepository voucherRepository;
    private final CustomerVoucherMapper customerVoucherMapper;
    private final Clock voucherClock;

    @Override
    @Transactional(readOnly = true)
    public List<CustomerVoucherResponse> getAllCustomerVouchers(String customerId, CustomerVoucherStatus status) {
        List<CustomerVoucher> wallet;
        if (customerId != null && status != null) {
            wallet = customerVoucherRepository.findByCustomerIdAndStatus(customerId, status);
        } else if (customerId != null) {
            wallet = customerVoucherRepository.findByCustomerId(customerId);
        } else if (status != null) {
            wallet = customerVoucherRepository.findByStatus(status);
        } else {
            wallet = customerVoucherRepository.findAll();
        }

        return wallet.stream()
                .map(customerVoucherMapper::toCustomerVoucherResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerVoucherResponse getCustomerVoucherById(Long id) {
        CustomerVoucher customerVoucher = customerVoucherRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No customer voucher with id " + id + " found in the system"));
        return customerVoucherMapper.toCustomerVoucherResponse(customerVoucher);
    }

    @Override
    @Transactional
    public CustomerVoucherResponse createCustomerVoucher(String customerId, Long voucherId) {
        Voucher voucher = voucherRepository.findById(voucherId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No voucher with id " + voucherId + " found in the system"));

        if (customerVoucherRepository.findByVoucherIdAndCustomerId(voucherId, customerId).isPresent()) {
            throw new ResourceAlreadyExistsException(
                    "Customer " + customerId + " already holds voucher " + voucherId);
        }

        VoucherBatch batch = voucher.getBatch();
        LocalDateTime now = LocalDateTime.now(voucherClock);

        CustomerVoucher customerVoucher = CustomerVoucher.builder()
                .voucher(voucher)
                .customerId(customerId)
                .status(CustomerVoucherStatus.AVAILABLE)
                .claimedAt(now)
                .expiredAt(calculateExpiredAt(now, batch.getDurationUnit(), batch.getDurationValue()))
                .build();

        try {
            customerVoucherRepository.saveAndFlush(customerVoucher);
        } catch (DataIntegrityViolationException ex) {
            throw new ResourceAlreadyExistsException(
                    "Customer " + customerId + " already holds voucher " + voucherId);
        }

        return customerVoucherMapper.toCustomerVoucherResponse(customerVoucher);
    }

    @Override
    @Transactional
    public CustomerVoucherResponse updateCustomerVoucherStatus(Long id, CustomerVoucherStatus status) {
        CustomerVoucher customerVoucher = customerVoucherRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No customer voucher with id " + id + " found in the system"));
        customerVoucher.setStatus(status);
        return customerVoucherMapper.toCustomerVoucherResponse(customerVoucher);
    }

    @Override
    @Transactional
    public void deleteCustomerVoucher(Long id) {
        CustomerVoucher customerVoucher = customerVoucherRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No customer voucher with id " + id + " found in the system"));
        customerVoucherRepository.delete(customerVoucher);
    }


}