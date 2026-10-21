package com.dacn.VoucherSystem.entity;

import com.dacn.VoucherSystem.enums.CampainStatus;
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
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private CampainStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id")
    private AdminAccount admin;

    @OneToMany(mappedBy = "campaign", cascade = CascadeType.ALL)
    private List<VoucherBatch> voucherBatches;

    @OneToMany(mappedBy = "campaign", cascade = CascadeType.ALL)
    private List<Voucher> vouchers;

    @OneToMany(mappedBy = "campaign", cascade = CascadeType.ALL)
    private List<DistributionLog> distributionLogs;
}
