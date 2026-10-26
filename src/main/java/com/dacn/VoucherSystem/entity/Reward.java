package com.dacn.VoucherSystem.entity;

import com.dacn.VoucherSystem.enums.RewardType;
import com.dacn.VoucherSystem.enums.RuleStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import tools.jackson.databind.JsonNode;

import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "rewards")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reward {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "reward_json", columnDefinition = "jsonb", nullable = false)
    private JsonNode rewardJson;

    @Enumerated(EnumType.STRING)
    @Column(name = "reward_type", nullable = false, length = 20)
    private RewardType rewardType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RuleStatus status;

    @OneToMany(mappedBy = "reward")
    private List<BatchRule> batchRules;
}
