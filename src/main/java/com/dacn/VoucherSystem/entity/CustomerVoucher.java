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
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voucher_id")
    private Voucher voucher;

    @Column(name = "customer_id", length = 50)
    private String customerId;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private CustomerVoucherStatus status;

    @Column(name = "expired_at")
    private LocalDateTime expiredAt;

    @Column(name = "claimed_at")
    private LocalDateTime claimedAt;
}
