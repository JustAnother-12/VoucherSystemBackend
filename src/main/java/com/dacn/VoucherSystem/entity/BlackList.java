package com.dacn.VoucherSystem.entity;

import com.dacn.VoucherSystem.enums.BlacklistType;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "black_lists")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BlackList {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "blacklist_type", length = 50, nullable = false)
    private BlacklistType blacklistType;

    @Column(name = "target_value", length = 255, nullable = false)
    private String targetValue;

    @Column(length = 255, nullable = false)
    private String reason;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Column(name = "expired_at")
    private LocalDateTime expiredAt;

    @Column(name = "created_at", insertable = false, nullable = false)
    private LocalDateTime createdAt;
}
