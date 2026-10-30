package com.dacn.VoucherSystem.security;

import com.dacn.VoucherSystem.entity.AdminAccount;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class CustomUserDetails implements UserDetails {
    private final AdminAccount adminAccount;

    public CustomUserDetails(AdminAccount adminAccount) {
        this.adminAccount = adminAccount;
    }

    // Expose your AdminAccount entity directly
    public AdminAccount getAdminAccount() {
        return adminAccount;
    }

    @Override
    public String getUsername() {
        return adminAccount.getUsername();
    }

    @Override
    public String getPassword() {
        return adminAccount.getHashedPassword();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(adminAccount.getRole().name()));
    }

    @Override
    public boolean isEnabled() {
        return adminAccount.getIsActive();
    }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }
}
