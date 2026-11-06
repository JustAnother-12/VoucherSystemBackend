package com.dacn.VoucherSystem.repository;

import com.dacn.VoucherSystem.entity.AdminAccount;
import com.dacn.VoucherSystem.entity.CustomerVoucher;
import com.dacn.VoucherSystem.enums.CustomerVoucherStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.EntityGraph;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.List;

@Repository
public interface CustomerVoucherRepository extends JpaRepository<CustomerVoucher, Long> {
    Optional<CustomerVoucher> findByVoucherIdAndCustomerId(
            Long voucherId,
            String customerId
    );

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
        update CustomerVoucher cv
           set cv.status = :used
           where cv.voucher.id = :voucherId
           and cv.customerId = :customerId
           and cv.status = :available
           and cv.expiredAt > :now
        """)
    int consumeAvailable(
            @Param("voucherId") Long voucherId,
            @Param("customerId") String customerId,
            @Param("available") CustomerVoucherStatus available,
            @Param("used") CustomerVoucherStatus used,
            @Param("now") LocalDateTime now
    );

    @Override
    @EntityGraph(attributePaths = "voucher")
    List<CustomerVoucher> findAll();

    @EntityGraph(attributePaths = "voucher")
    List<CustomerVoucher> findByCustomerId(String customerId);

    @EntityGraph(attributePaths = "voucher")
    List<CustomerVoucher> findByStatus(CustomerVoucherStatus status);

    @EntityGraph(attributePaths = "voucher")
    List<CustomerVoucher> findByCustomerIdAndStatus(String customerId, CustomerVoucherStatus status);
}
