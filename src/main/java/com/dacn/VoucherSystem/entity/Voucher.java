package com.dacn.VoucherSystem.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "vouchers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Voucher {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id")
    private VoucherBatch batch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id")
    private Campaign campaign;

    @Column(nullable = false, length = 50)
    private String code;

    @Column(name = "created_at", insertable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "voucher")
    private List<CustomerVoucher> customerVouchers;

    @OneToMany(mappedBy = "voucher")
    private List<RedemptionLog> redemptionLogs;

    @OneToMany(mappedBy = "voucher")
    private List<FraudLog> fraudLogs;

    @OneToMany(mappedBy = "voucher")
    private List<DistributionLog> distributionLogs;
}
