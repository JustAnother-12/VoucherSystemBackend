package com.dacn.VoucherSystem.repository;

import com.dacn.VoucherSystem.entity.AdminAccount;
import com.dacn.VoucherSystem.entity.CustomerVoucher;
import com.dacn.VoucherSystem.enums.CustomerVoucherStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

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
}
