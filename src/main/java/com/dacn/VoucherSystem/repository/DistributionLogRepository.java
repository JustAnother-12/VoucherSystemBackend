package com.dacn.VoucherSystem.repository;

import com.dacn.VoucherSystem.entity.DistributionLog;
import com.dacn.VoucherSystem.entity.Voucher;
import com.dacn.VoucherSystem.entity.VoucherBatch;
import com.dacn.VoucherSystem.enums.DistributionStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DistributionLogRepository extends JpaRepository<DistributionLog, Long> {

    @Query("""
            select d from DistributionLog d
            join fetch d.voucher
            join fetch d.campaign
            where (:voucherId is null or d.voucher.id = :voucherId)
              and (:campaignId is null or d.campaign.id = :campaignId)
              and (:status is null or d.status = :status)
            order by d.sentAt desc, d.id desc
            """)
    List<DistributionLog> search(
            @Param("voucherId") Long voucherId,
            @Param("campaignId") Long campaignId,
            @Param("status") DistributionStatus status
    );

    @Query("""
            select d from DistributionLog d
            join fetch d.voucher
            join fetch d.campaign
            where d.id = :id
            """)
    Optional<DistributionLog> findDetailById(@Param("id") Long id);

    /** Lấy batch và đảm bảo batch thuộc đúng campaign. */
    @Query("""
            select b from VoucherBatch b
            where b.id = :batchId and b.campaign.id = :campaignId
            """)
    Optional<VoucherBatch> findBatchInCampaign(
            @Param("batchId") Long batchId,
            @Param("campaignId") Long campaignId
    );

    /**
     * Số lượt phân phối đã dùng quota của một kênh trong batch (PENDING hoặc SENT, bỏ FAILED).
     */
    @Query("""
            select count(d) from DistributionLog d
            where d.voucher.batch.id = :batchId
              and d.channel = :channel
              and d.status <> com.dacn.VoucherSystem.enums.DistributionStatus.FAILED
            """)
    long countActiveByBatchAndChannel(
            @Param("batchId") Long batchId,
            @Param("channel") String channel
    );

    /**
     +     * Tổng số lượt phân phối (PENDING hoặc SENT) của batch, mọi kênh, bỏ FAILED.
     +     * Dùng để so với maxUsageCount của batch.
     +     */
    @Query("""
            select count(d) from DistributionLog d
            where d.voucher.batch.id = :batchId
              and d.status <> com.dacn.VoucherSystem.enums.DistributionStatus.FAILED
            """)
    long countActiveByBatch(@Param("batchId") Long batchId);

    /**
     * BULK: voucher của batch chưa được phân phối (log FAILED thì được gửi lại).
     */
    @Query("""
            select v from Voucher v
            join fetch v.campaign
            where v.batch.id = :batchId
              and v.campaign.id = :campaignId
              and not exists (
                  select 1 from DistributionLog d
                  where d.voucher = v
                    and d.status <> com.dacn.VoucherSystem.enums.DistributionStatus.FAILED
              )
            order by v.id asc
            """)
    List<Voucher> findAvailableVouchers(
            @Param("batchId") Long batchId,
            @Param("campaignId") Long campaignId,
            Pageable pageable
    );

    /**
     * GENERIC: lấy voucher của batch, không loại theo log vì một mã được gửi cho nhiều người.
     */
    @Query("""
            select v from Voucher v
            join fetch v.campaign
            where v.batch.id = :batchId
              and v.campaign.id = :campaignId
            order by v.id asc
            """)
    List<Voucher> findVouchersOfBatch(
            @Param("batchId") Long batchId,
            @Param("campaignId") Long campaignId,
            Pageable pageable
    );

    /**
     * GENERIC: đã có lượt gửi (PENDING hoặc SENT) mã này cho cùng địa chỉ trên cùng kênh chưa.
     */
    @Query("""
            select count(d) > 0 from DistributionLog d
            where d.voucher.id = :voucherId
              and d.channel = :channel
              and d.targetAddress = :targetAddress
              and d.status <> com.dacn.VoucherSystem.enums.DistributionStatus.FAILED
            """)
    boolean existsActiveDistribution(
            @Param("voucherId") Long voucherId,
            @Param("channel") String channel,
            @Param("targetAddress") String targetAddress
    );
}
