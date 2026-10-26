package com.dacn.VoucherSystem.entity;

import com.dacn.VoucherSystem.enums.CampaignStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "campaigns")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Campaign {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "campaign_name", nullable = false, length = 100)
    private String campaignName;

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private CampaignStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id")
    private AdminAccount admin;

    @Column(name = "created_at", insertable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "campaign", cascade = CascadeType.ALL)
    private List<VoucherBatch> voucherBatches;

    @OneToMany(mappedBy = "campaign", cascade = CascadeType.ALL)
    private List<Voucher> vouchers;

    @OneToMany(mappedBy = "campaign", cascade = CascadeType.ALL)
    private List<DistributionLog> distributionLogs;
}
