package com.dacn.VoucherSystem.service.impl;

import com.dacn.VoucherSystem.dto.response.VoucherResponse;
import com.dacn.VoucherSystem.entity.Voucher;
import com.dacn.VoucherSystem.exception.ResourceNotFoundException;
import com.dacn.VoucherSystem.mapper.VoucherMapper;
import com.dacn.VoucherSystem.repository.VoucherRepository;
import com.dacn.VoucherSystem.service.interfaces.VoucherService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VoucherServiceImpl implements VoucherService {
    private final VoucherRepository voucherRepository;
    private final VoucherMapper voucherMapper;

    @Override
    @Transactional(readOnly = true)
    public List<VoucherResponse> getAllVouchers(Long batchId, Long campaignId) {
        List<Voucher> vouchers;
        if (batchId != null && campaignId != null) {
            vouchers = voucherRepository.findByBatchIdAndCampaignId(batchId, campaignId);
        } else if (batchId != null) {
            vouchers = voucherRepository.findByBatchId(batchId);
        } else if (campaignId != null) {
            vouchers = voucherRepository.findByCampaignId(campaignId);
        } else {
            vouchers = voucherRepository.findAll();
        }

        return vouchers.stream()
                .map(voucherMapper::toVoucherResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public VoucherResponse getVoucherById(Long id) {
        Voucher voucher = voucherRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No voucher with id " + id + " found in the system"));
        return voucherMapper.toVoucherResponse(voucher);
    }

    @Override
    @Transactional(readOnly = true)
    public VoucherResponse getVoucherByCode(String code) {
        Voucher voucher = voucherRepository.findVoucherByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No voucher with code " + code + " found in the system"));
        return voucherMapper.toVoucherResponse(voucher);
    }
}