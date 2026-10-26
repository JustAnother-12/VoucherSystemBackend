package com.dacn.VoucherSystem.entity;

import com.dacn.VoucherSystem.enums.DistributionStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "distribution_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DistributionLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id", nullable = false)
    private Campaign campaign;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voucher_id", nullable = false)
    private Voucher voucher;

    @Column(length = 50, nullable = false)
    private String channel;

    @Column(name = "target_address", length = 255, nullable = false)
    private String targetAddress;

    @Enumerated(EnumType.STRING)
    @Column(length = 50, nullable = false)
    private DistributionStatus status;

    @Column(name = "sent_at", nullable = false)
    private LocalDateTime sentAt;
}
