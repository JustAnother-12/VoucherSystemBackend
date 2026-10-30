package com.dacn.VoucherSystem.controller;

import com.dacn.VoucherSystem.dto.request.LoginRequest;
import com.dacn.VoucherSystem.dto.request.RefreshTokenRequest;
import com.dacn.VoucherSystem.dto.response.ApiResponse;
import com.dacn.VoucherSystem.dto.response.AuthResponse;
import com.dacn.VoucherSystem.service.AuthService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
@Tag(name = "Authentication", description = "Operations pertaining login, refresh token and changing passwords")
public class AuthController {
    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid
            @RequestBody
            LoginRequest request
    ) {
        AuthResponse response = authService.login(
                request.getUsername(),
                request.getPassword()
        );
        return ResponseEntity.ok(
                ApiResponse.<AuthResponse>builder()
                        .status(200)
                        .message("Login successful")
                        .data(response)
                        .build()
        );
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<String>> logout(
            @Valid
            @RequestBody
            RefreshTokenRequest request
    ) {
        authService.logout(request.getRefreshToken());
        return ResponseEntity.ok(
                ApiResponse.<String>builder()
                        .status(200)
                        .message("Logout successfully")
                        .data("Session revoked")
                        .build()
        );
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(
            @Valid
            @RequestBody
            RefreshTokenRequest request
    ) {
        AuthResponse response = authService.refreshToken(request.getRefreshToken());
        return ResponseEntity.ok(
                ApiResponse.<AuthResponse>builder()
                        .status(200)
                        .message("Token refreshed successfully")
                        .data(response)
                        .build()
        );
    }
}
