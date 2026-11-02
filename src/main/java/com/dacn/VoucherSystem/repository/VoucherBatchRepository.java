package com.dacn.VoucherSystem.repository;

import com.dacn.VoucherSystem.entity.VoucherBatch;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;

@Repository
public interface VoucherBatchRepository extends JpaRepository<VoucherBatch, Long> {
    @Modifying
    @Query(value = """
    UPDATE voucher_batches b
       SET redeemed_count = b.redeemed_count + 1
     WHERE b.id = :batchId
       AND b.status = 'GENERATED'
       AND b.redeemed_count < b.generated_count
       AND EXISTS (
           SELECT 1
             FROM vouchers v
            WHERE v.id = :voucherId
              AND v.batch_id = b.id
              AND v.campaign_id = b.campaign_id
       )
       AND EXISTS (
           SELECT 1
             FROM campaigns c
            WHERE c.id = b.campaign_id
              AND c.status = 'ACTIVE'
              AND c.start_time <= :now
              AND c.end_time > :now
       )
       AND EXISTS (
           SELECT 1
             FROM batch_rules br
             JOIN conditions co ON co.id = br.condition_id
             JOIN rewards r ON r.id = br.reward_id
            WHERE br.id = b.rule_id
              AND co.status = 'PUBLISHED'
              AND r.status = 'PUBLISHED'
       )
    """, nativeQuery = true)
    int tryConsumeRedemptionQuota(
            @Param("batchId") Long batchId,
            @Param("voucherId") Long voucherId,
            @Param("now") LocalDateTime now
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from VoucherBatch b where b.id = :id")
    Optional<VoucherBatch> findByIdForUpdate(@Param("id") Long id);
}
