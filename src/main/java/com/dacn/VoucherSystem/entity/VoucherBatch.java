package com.dacn.VoucherSystem.entity;

import com.dacn.VoucherSystem.enums.BatchType;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "voucher_batches")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VoucherBatch {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id")
    private Campaign campaign;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rule_id", unique = true)
    private BatchRule rule;

    @Enumerated(EnumType.STRING)
    @Column(name = "batch_type", length = 50)
    private BatchType batchType;

    @Column(name = "code_config_json", columnDefinition = "jsonb")
    private String codeConfigJson;

    @Column(name = "duration_value")
    private Integer durationValue;

    @Column(name = "duration_unit", length = 20)
    private String durationUnit;

    @Column(name = "generated_count")
    private Integer generatedCount;

    @Column(name = "redeemed_count")
    private Integer redeemedCount;

    @Column(name = "distribution_channels_json", columnDefinition = "jsonb")
    private String distributionChannelsJson;

    @OneToMany(mappedBy = "batch", cascade = CascadeType.ALL)
    private List<Voucher> vouchers;
}
