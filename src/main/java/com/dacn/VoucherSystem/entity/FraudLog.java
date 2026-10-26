package com.dacn.VoucherSystem.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "fraud_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FraudLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voucher_id", nullable = false)
    private Voucher voucher;

    @Column(name = "customer_id", length = 50, nullable = false)
    private String customerId;

    @Column(name = "ip_address", length = 45, nullable = false)
    private String ipAddress;

    @Column(name = "device_fingerprint", length = 255, nullable = false)
    private String deviceFingerprint;

    @Column(name = "violation_rule", length = 255, nullable = false)
    private String violationRule;

    @Column(name = "created_at", insertable = false, nullable = false)
    private LocalDateTime createdAt;
}
