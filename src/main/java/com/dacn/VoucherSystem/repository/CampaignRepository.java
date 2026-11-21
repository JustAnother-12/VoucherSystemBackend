package com.dacn.VoucherSystem.repository;

import com.dacn.VoucherSystem.entity.Campaign;
import com.dacn.VoucherSystem.enums.CampaignStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface CampaignRepository extends JpaRepository<Campaign, Long> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE Campaign c
            SET c.status = :active
            WHERE c.status <> :paused
              AND c.status <> :active
              AND c.startTime <= :now
              AND c.endTime >= :now
            """)
    int activateCampaignsInTimeWindow(
            @Param("now") LocalDateTime now,
            @Param("active") CampaignStatus active,
            @Param("paused") CampaignStatus paused
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE Campaign c
            SET c.status = :ended
            WHERE c.status <> :paused
              AND c.status <> :ended
              AND c.endTime < :now
            """)
    int endExpiredCampaigns(
            @Param("now") LocalDateTime now,
            @Param("ended") CampaignStatus ended,
            @Param("paused") CampaignStatus paused
    );
}
