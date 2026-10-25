package com.dacn.VoucherSystem.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "conditions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Condition {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "condition_json", columnDefinition = "jsonb")
    private String conditionJson;

    @OneToMany(mappedBy = "condition")
    private List<BatchRule> batchRules;
}
