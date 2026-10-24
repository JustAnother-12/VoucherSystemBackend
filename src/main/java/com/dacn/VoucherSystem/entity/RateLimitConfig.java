package com.dacn.VoucherSystem.entity;

import com.dacn.VoucherSystem.enums.ActionOnExceed;
import com.dacn.VoucherSystem.enums.LimitScope;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "rate_limit_configs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RateLimitConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "endpoint_pattern", length = 255)
    private String endpointPattern;

    @Enumerated(EnumType.STRING)
    @Column(name = "limit_scope", length = 50)
    private LimitScope limitScope;

    @Column(name = "max_request")
    private Integer maxRequest;

    @Column(name = "time_window_seconds")
    private Integer timeWindowSeconds;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_on_exceed", length = 50)
    private ActionOnExceed actionOnExceed;

    private Integer priority;

    @Column(name = "is_active")
    private Boolean isActive;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
