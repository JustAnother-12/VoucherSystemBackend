package com.dacn.VoucherSystem.service;

import com.dacn.VoucherSystem.enums.CustomerVoucherStatus;
import com.dacn.VoucherSystem.repository.CustomerVoucherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CustomerVoucherStatusUpdater {
    private final CustomerVoucherRepository customerVoucherRepository;
    private final Clock clock;

    @Scheduled(fixedDelayString = "${customer-voucher.status-update.fixed-delay:3600000}")
    @Transactional
    public void expireCustomerVouchers() {
        customerVoucherRepository.expireAvailableOrUnavailableCustomerVouchers(
                CustomerVoucherStatus.AVAILABLE,
                CustomerVoucherStatus.UNAVAILABLE,
                CustomerVoucherStatus.EXPIRED,
                LocalDateTime.now(clock)
        );
    }
}
