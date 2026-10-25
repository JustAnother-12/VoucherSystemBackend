package com.dacn.VoucherSystem.security;

import com.dacn.VoucherSystem.entity.AdminAccount;
import com.dacn.VoucherSystem.repository.AdminAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomUserDetailService implements UserDetailsService {
    private final AdminAccountRepository adminAccountRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        AdminAccount account =
                adminAccountRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Account not found for username: " + username));

        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(account.getRole().name()));

        return new org.springframework.security.core.userdetails.User(
                account.getUsername(),
                account.getHashedPassword(),
                account.getIsActive(),
                true,
                true,
                true,
                authorities
        );
    }
}
