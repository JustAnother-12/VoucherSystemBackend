package com.dacn.VoucherSystem.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
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

    @Column(name = "reward_name", nullable = false, length = 100)
    private String rewardName;

    @Column(name = "reward_config_json", columnDefinition = "jsonb")
    private String rewardJson;

    @Column(name = "created_at", insertable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "reward")
    private List<BatchRule> batchRules;
}
