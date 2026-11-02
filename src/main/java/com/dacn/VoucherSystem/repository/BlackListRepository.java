package com.dacn.VoucherSystem.repository;

import com.dacn.VoucherSystem.entity.AdminAccount;
import com.dacn.VoucherSystem.entity.BlackList;
import com.dacn.VoucherSystem.enums.BlacklistType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface BlackListRepository extends JpaRepository<BlackList, Long> {
    @Query("""
        SELECT COUNT(b) > 0
        FROM BlackList b
        WHERE b.blacklistType = :type
          AND b.targetValue = :value
          AND b.isActive = true
          AND (b.expiredAt IS NULL OR b.expiredAt > :now)
    """)
    boolean existsActive(
            @Param("type") BlacklistType type,
            @Param("value") String value,
            @Param("now") LocalDateTime now
    );

    @Query("""
    SELECT b
    FROM BlackList b
    WHERE b.isActive = true
      AND (b.expiredAt IS NULL OR b.expiredAt > :now)
      AND (
          (:customerId IS NOT NULL
              AND b.blacklistType = :customerType
              AND b.targetValue = :customerId)
          OR
          (:ipAddress IS NOT NULL
              AND b.blacklistType = :ipType
              AND b.targetValue = :ipAddress)
          OR
          (:deviceFingerprint IS NOT NULL
              AND b.blacklistType = :deviceType
              AND b.targetValue = :deviceFingerprint)
      )
    """)
    List<BlackList> findActiveMatches(
            @Param("customerType") BlacklistType customerType,
            @Param("customerId") String customerId,
            @Param("ipType") BlacklistType ipType,
            @Param("ipAddress") String ipAddress,
            @Param("deviceType") BlacklistType deviceType,
            @Param("deviceFingerprint") String deviceFingerprint,
            @Param("now") LocalDateTime now
    );
}
