package com.dacn.VoucherSystem.service.impl;

import com.dacn.VoucherSystem.dto.response.AuthResponse;
import com.dacn.VoucherSystem.entity.AdminAccount;
import com.dacn.VoucherSystem.entity.Session;
import com.dacn.VoucherSystem.exception.InvalidOrExpiredTokenException;
import com.dacn.VoucherSystem.exception.ResourceNotFoundException;
import com.dacn.VoucherSystem.repository.SessionRepository;
import com.dacn.VoucherSystem.security.CustomUserDetails;
import com.dacn.VoucherSystem.security.JwtUtil;
import com.dacn.VoucherSystem.security.TokenHasher;
import com.dacn.VoucherSystem.service.AuthService;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final SessionRepository sessionRepository;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil tokenProvider;
    private final TokenHasher tokenHasher;

    @Override
    public AuthResponse login(
            String username,
            String password
    ) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        username,
                        password
                )
        );

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        AdminAccount account = userDetails.getAdminAccount();
        String accessToken = tokenProvider.generateAccessToken(account.getUsername());
        String refreshToken = tokenProvider.generateRefreshToken(account.getUsername());

        createSession(account, refreshToken);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .accessExpiresAt(LocalDateTime.now().plusHours(tokenProvider.jwtAccessExpirationHours))
                .refreshExpiresAt(LocalDateTime.now().plusDays(tokenProvider.jwtRefreshExpirationDays))
                .tokenType("Bearer")
                .build();
    }

    @Override
    public void logout(String refreshToken) {
        String hashedToken = tokenHasher.hashToken(refreshToken);
        Session session = sessionRepository
                .findSessionByRefreshTokenHash(hashedToken)
                .filter(s -> s.getRevokedAt() == null)
                .orElseThrow(
                        () -> new ResourceNotFoundException("No active session found for the provided refresh token")
                );
        session.setRevokedAt(LocalDateTime.now());
        sessionRepository.saveAndFlush(session);
    }

    @Override
    public AuthResponse refreshToken(String refreshToken) {
        if(tokenProvider.validateToken(refreshToken)) {
            Claims claims = tokenProvider.getAllClaimsFromToken(refreshToken);

            String tokenType = claims.get("token_type", String.class);
            if (!"REFRESH".equals(tokenType)) {
                throw new InvalidOrExpiredTokenException("Invalid token type. Expected a refresh token.");
            }

            String hashedToken = tokenHasher.hashToken(refreshToken);
            Session session = sessionRepository
                    .findSessionByRefreshTokenHash(hashedToken)
                    .filter(s -> s.getRevokedAt() == null)
                    .orElseThrow(
                            () -> new ResourceNotFoundException("No active session with the requested refresh token found in the system")
                    );

            AdminAccount account = session.getAccount();
            String username = account.getUsername();

            // create a new set of refresh and access token
            String newAccessToken = tokenProvider.generateAccessToken(username);
            String newRefreshToken = tokenProvider.generateRefreshToken(username);

            // revoke the old session
            session.setRevokedAt(LocalDateTime.now());
            sessionRepository.saveAndFlush(session);

            createSession(session.getAccount(), newRefreshToken);

            return AuthResponse.builder()
                    .accessToken(newAccessToken)
                    .refreshToken(newRefreshToken)
                    .accessExpiresAt(LocalDateTime.now().plusHours(tokenProvider.jwtAccessExpirationHours))
                    .refreshExpiresAt(LocalDateTime.now().plusDays(tokenProvider.jwtRefreshExpirationDays))
                    .tokenType("Bearer")
                    .build();
        }
        throw new InvalidOrExpiredTokenException("Refresh token has expired or is invalid");
    }

    @Override
    public Session createSession(AdminAccount account, String refreshToken) {
        String hashedRefreshToken  = tokenHasher.hashToken(refreshToken);
        Session session = Session.builder()
                .account(account)
                .refreshTokenHash(hashedRefreshToken)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusDays(tokenProvider.jwtRefreshExpirationDays))
                .revokedAt(null)
                .build();

        sessionRepository.saveAndFlush(session);
        return session;
    }
}
