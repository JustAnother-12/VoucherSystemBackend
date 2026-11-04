package com.dacn.VoucherSystem.repository;

import com.dacn.VoucherSystem.entity.Voucher;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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


    // Avoid generating duplicate codes for batch type = BULK.
    boolean existsByCode(String code);

    @Query("select v.code from Voucher v where v.code in :codes")
    Set<String> findExistingCodes(@Param("codes") Collection<String> codes);

    List<Voucher> findByBatchId(Long batchId);
}
