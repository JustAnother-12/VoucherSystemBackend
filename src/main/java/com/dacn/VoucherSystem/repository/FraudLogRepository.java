package com.dacn.VoucherSystem.repository;

import com.dacn.VoucherSystem.entity.AdminAccount;
import com.dacn.VoucherSystem.entity.FraudLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface FraudLogRepository extends JpaRepository<FraudLog, Long> {
}
