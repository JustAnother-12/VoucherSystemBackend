package com.dacn.VoucherSystem.entity;

import com.dacn.VoucherSystem.enums.RedemptionStatus;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "redemption_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RedemptionLog {
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

    @Column(name = "original_amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal originalAmount;

    @Column(name = "discount_amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal discountAmount;

    @Column(name = "final_amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal finalAmount;

    @Enumerated(EnumType.STRING)
    @Column(length = 50, nullable = false)
    private RedemptionStatus status;

    @Column(name = "failure_reason", length = 255, nullable = false)
    private String failureReason;

    @Column(name = "created_at", insertable = false, nullable = false)
    private LocalDateTime createdAt;
}
