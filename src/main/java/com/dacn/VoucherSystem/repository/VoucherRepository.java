package com.dacn.VoucherSystem.repository;

import com.dacn.VoucherSystem.entity.Voucher;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VoucherRepository extends JpaRepository<Voucher, Long> {
    @EntityGraph(attributePaths = {
            "campaign",
            "batch",
            "batch.campaign",
            "batch.rule",
            "batch.rule.condition",
            "batch.rule.reward"
    })
    Optional<Voucher> findVoucherByCode(String code);
}
