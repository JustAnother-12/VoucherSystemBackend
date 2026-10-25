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
    @JoinColumn(name = "campaign_id")
    private Campaign campaign;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voucher_id")
    private Voucher voucher;

    @Column(length = 50)
    private String channel;

    @Column(name = "target_address", length = 255)
    private String targetAddress;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private DistributionStatus status;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;
}
