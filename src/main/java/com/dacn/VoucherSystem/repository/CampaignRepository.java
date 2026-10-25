package com.dacn.VoucherSystem.repository;

import com.dacn.VoucherSystem.entity.Campaign;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface CampaignRepository extends JpaRepository<Campaign, Long> {
}
