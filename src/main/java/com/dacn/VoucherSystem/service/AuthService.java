package com.dacn.VoucherSystem.service;

import com.dacn.VoucherSystem.dto.request.LoginRequest;
import com.dacn.VoucherSystem.dto.request.RefreshTokenRequest;
import com.dacn.VoucherSystem.dto.response.AuthResponse;

public interface AuthService {
    public AuthResponse login(
            String username,
            String password
    );
    public AuthResponse refreshToken(String refreshToken);
}
