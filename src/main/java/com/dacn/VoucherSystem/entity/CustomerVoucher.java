package com.dacn.VoucherSystem.entity;

import com.dacn.VoucherSystem.enums.CustomerVoucherStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "customer_vouchers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerVoucher {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voucher_id", nullable = false)
    private Voucher voucher;

    @Column(name = "customer_id", length = 50, nullable = false)
    private String customerId;

    @Enumerated(EnumType.STRING)
    @Column(length = 50, nullable = false)
    private CustomerVoucherStatus status;

    @Column(name = "expired_at", nullable = false)
    private LocalDateTime expiredAt;

    @Column(name = "claimed_at", insertable = false, nullable = false)
    private LocalDateTime claimedAt;
}
