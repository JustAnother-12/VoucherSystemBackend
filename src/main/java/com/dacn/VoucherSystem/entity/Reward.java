package com.dacn.VoucherSystem.entity;

import jakarta.persistence.*;
import lombok.*;

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

    @Column(name = "reward_json", columnDefinition = "jsonb")
    private String rewardJson;

    @OneToMany(mappedBy = "reward")
    private List<BatchRule> batchRules;
}
