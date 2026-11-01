package com.dacn.VoucherSystem.repository;

import com.dacn.VoucherSystem.entity.AdminAccount;
import com.dacn.VoucherSystem.entity.RedemptionLog;
import com.dacn.VoucherSystem.enums.RedemptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface RedemptionLogRepository extends JpaRepository<RedemptionLog, Long> {
    boolean existsByVoucherIdAndCustomerIdAndStatus(
            Long voucherId,
            String customerId,
            RedemptionStatus status
    );

    long countByVoucherIdAndCustomerIdAndStatus(
            Long voucherId,
            String customerId,
            RedemptionStatus status
    );
}
