package com.dacn.VoucherSystem.repository;

import com.dacn.VoucherSystem.entity.BlackList;
import com.dacn.VoucherSystem.enums.BlacklistType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

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
    """)
    List<BlackList> findCurrentlyActive(
            @Param("now") LocalDateTime now
    );
}
