package com.dacn.VoucherSystem.repository;

import com.dacn.VoucherSystem.entity.AdminAccount;
import com.dacn.VoucherSystem.entity.RedemptionLog;
import com.dacn.VoucherSystem.enums.RedemptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

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

    @Query("""
        SELECT r.voucher.id, COUNT(r.id) 
        FROM RedemptionLog r 
        WHERE r.voucher.id IN :voucherIds AND r.status = :status 
        GROUP BY r.voucher.id
    """)
    List<Object[]> countLogsByVoucherIdsAndStatus(
            @Param("voucherIds") List<Long> voucherIds,
            @Param("status") RedemptionStatus status
    );

    default Map<Long, Long> countSuccessLogsByVoucherIds(List<Long> voucherIds) {
        List<Object[]> results = countLogsByVoucherIdsAndStatus(voucherIds, RedemptionStatus.SUCCESS);
        return results.stream().collect(Collectors.toMap(
                row -> (Long) row[0],
                row -> (Long) row[1]
        ));
    }

    @Query("""
        SELECT 
            r.voucher.id,
            SUM(r.originalAmount),
            SUM(r.discountAmount),
            SUM(r.finalAmount)
        FROM RedemptionLog r
        WHERE r.voucher.id IN :voucherIds AND r.status = com.dacn.VoucherSystem.enums.RedemptionStatus.SUCCESS
        GROUP BY r.voucher.id
    """)
    List<Object[]> findRawRevenueByVoucherIds(@Param("voucherIds") List<Long> voucherIds);

    default Map<Long, Object[]> aggregateRevenueByVoucherIds(List<Long> voucherIds) {
        List<Object[]> results = findRawRevenueByVoucherIds(voucherIds);
        return results.stream().collect(Collectors.toMap(
                row -> (Long) row[0],
                row -> new Object[]{ row[1], row[2], row[3] }
        ));
    }

    @Query("""
    SELECT 
        SUM(r.originalAmount),
        SUM(r.discountAmount),
        SUM(r.finalAmount)
    FROM RedemptionLog r
    WHERE r.voucher.batch.id = :batchId 
      AND r.status = com.dacn.VoucherSystem.enums.RedemptionStatus.SUCCESS
""")
    List<Object[]> findRawRevenueByBatchId(@Param("batchId") Long batchId);

    default Object[] aggregateRevenueByBatchId(Long batchId) {
        List<Object[]> results = findRawRevenueByBatchId(batchId);
        if (results.isEmpty() || results.get(0) == null) {
            return null;
        }
        return results.get(0);
    }
}
