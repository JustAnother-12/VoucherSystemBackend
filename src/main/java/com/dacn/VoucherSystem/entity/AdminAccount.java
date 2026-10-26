package com.dacn.VoucherSystem.entity;

import com.dacn.VoucherSystem.enums.AdminRole;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "admin_accounts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminAccount {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(name = "hashed_password", nullable = false, length = 255)
    private String hashedPassword;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_role", nullable = false, length = 20)
    private AdminRole role;

    @Column(name = "created_at", insertable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "admin", cascade = CascadeType.ALL)
    private List<Campaign> campaigns;
}
