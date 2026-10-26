package com.dacn.VoucherSystem.entity;

import com.dacn.VoucherSystem.enums.BatchStatus;
import com.dacn.VoucherSystem.enums.BatchType;
import com.dacn.VoucherSystem.enums.DurationUnit;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import tools.jackson.databind.JsonNode;

import java.time.LocalDateTime;
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
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id")
    private Campaign campaign;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rule_id", unique = true)
    private BatchRule rule;

    @Enumerated(EnumType.STRING)
    @Column(name = "batch_type", length = 50)
    private BatchType batchType;

    @Column(name = "code_config_json", columnDefinition = "jsonb")
    private String codeConfigJson;

    @Column(name = "duration_value")
    private Integer durationValue;

    @Enumerated(EnumType.STRING)
    @Column(name = "duration_unit", length = 20)
    private DurationUnit durationUnit;

    @Column(name = "generated_count")
    private Integer generatedCount;

    @Column(name = "redeemed_count")
    private Integer redeemedCount;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "distribution_channels_json", columnDefinition = "jsonb")
    private JsonNode distributionChannelsJson;

    @OneToMany(mappedBy = "batch", cascade = CascadeType.ALL)
    private List<Voucher> vouchers;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private BatchStatus status;

    @Column(name = "created_at", insertable = false)
    private LocalDateTime createdAt;
}
