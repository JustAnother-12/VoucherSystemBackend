package com.dacn.VoucherSystem.repository;

import com.dacn.VoucherSystem.entity.AdminAccount;
import com.dacn.VoucherSystem.entity.RateLimitConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface RateLimitConfigRepository extends JpaRepository<RateLimitConfig, Long> {
}
