package com.dacn.VoucherSystem.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "black_list")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BlackList {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(length = 50)
    private String type;

    @Column(name = "\"value\"", length = 255)
    private String value;

    @Column(length = 255)
    private String reason;

    @Column(name = "is_active")
    private Boolean isActive;

    @Column(name = "expired_at")
    private LocalDateTime expiredAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
