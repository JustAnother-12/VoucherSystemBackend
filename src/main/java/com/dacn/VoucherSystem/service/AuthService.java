package com.dacn.VoucherSystem.service;

import com.dacn.VoucherSystem.dto.request.LoginRequest;
import com.dacn.VoucherSystem.dto.request.RefreshTokenRequest;
import com.dacn.VoucherSystem.dto.response.AuthResponse;
import com.dacn.VoucherSystem.entity.AdminAccount;
import com.dacn.VoucherSystem.entity.Session;

public interface AuthService {
    public AuthResponse login(
            String username,
            String password
    );
    public void logout(String refreshToken);
    public AuthResponse refreshToken(String refreshToken);
    public Session createSession(AdminAccount account, String tokenHash);
}
