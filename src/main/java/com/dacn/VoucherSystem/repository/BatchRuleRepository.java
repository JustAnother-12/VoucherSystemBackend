package com.dacn.VoucherSystem.repository;

import com.dacn.VoucherSystem.entity.AdminAccount;
import com.dacn.VoucherSystem.entity.BatchRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface BatchRuleRepository extends JpaRepository<BatchRule, Long>, JpaSpecificationExecutor<BatchRule> {

    boolean existsByRewardIdAndConditionId(Long rewardId, Long conditionId);
}
