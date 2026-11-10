package com.dacn.VoucherSystem.repository;

import com.dacn.VoucherSystem.entity.VoucherBatch;
import com.dacn.VoucherSystem.enums.BatchStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface VoucherBatchRepository extends JpaRepository<VoucherBatch, Long> {

    @Modifying(flushAutomatically = true)
    @Query(value = """
        UPDATE voucher_batch_redemption_counters counter
            SET redeemed_count = counter.redeemed_count + 1
    
        WHERE counter.batch_id = :batchId
           AND counter.shard_id = :shardId
           AND counter.redeemed_count < counter.shard_limit
    
           AND EXISTS (
               SELECT 1
               FROM voucher_batches b
    
               WHERE b.id = counter.batch_id
                 AND b.status = 'GENERATED'
    
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
                     JOIN conditions co
                         ON co.id = br.condition_id
                     JOIN rewards r
                         ON r.id = br.reward_id
    
                     WHERE br.id = b.rule_id
                       AND co.status = 'PUBLISHED'
                       AND r.status = 'PUBLISHED'
                 )
           )
        """, nativeQuery = true)
    int tryConsumeRedemptionQuotaOnShard(
            @Param("batchId") Long batchId,
            @Param("voucherId") Long voucherId,
            @Param("shardId") int shardId,
            @Param("now") LocalDateTime now
    );

    boolean existsByRule_IdAndStatus(Long ruleId, BatchStatus status);


    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from VoucherBatch b where b.id = :id")
    Optional<VoucherBatch> findByIdForUpdate(@Param("id") Long id);

    List<VoucherBatch> findByCampaignId(Long campaignId);

    Optional<VoucherBatch> findByIdAndCampaignId(Long id, Long campaignId);
}
