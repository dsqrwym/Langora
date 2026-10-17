package org.dsqrwym.langora.auth.admin.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dsqrwym.langora.audit.OperationLogRepository;
import org.dsqrwym.langora.audit.auth.AuthAuditEvents;
import org.dsqrwym.langora.auth.admin.config.AdminSessionProperties;
import org.dsqrwym.langora.auth.dto.LoginRequestDto;
import org.dsqrwym.langora.auth.dto.LoginResponseDto;
import org.dsqrwym.langora.auth.admin.repository.AuthAdminRepository;
import org.dsqrwym.langora.auth.admin.repository.AuthAdminSessionRepository;
import org.dsqrwym.langora.auth.dto.RefreshTokenResponse;
import org.dsqrwym.langora.auth.jwt.GeneratedToken;
import org.dsqrwym.langora.auth.jwt.JwtTokenService;
import org.dsqrwym.langora.auth.jwt.UserJwtPayload;
import org.dsqrwym.langora.infrastructure.jooq.enums.AdminSessionRevocationReason;
import org.dsqrwym.langora.infrastructure.jooq.enums.AdminUserStatus;
import org.dsqrwym.langora.infrastructure.security.TimingAttackMitigation;
import org.dsqrwym.langora.infrastructure.util.HashUtils;
import org.dsqrwym.langora.infrastructure.web.ErrorCode;
import org.dsqrwym.langora.infrastructure.web.exception.ConflictException;
import org.dsqrwym.langora.infrastructure.web.exception.ForbiddenException;
import org.dsqrwym.langora.infrastructure.web.exception.UnauthorizedException;
import org.jspecify.annotations.NonNull;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthAdminService {
    private final TransactionTemplate tx;
    private final AdminSessionProperties adminSessionProperties;
    private final AuthAdminRepository adminRepository;
    private final AuthAdminSessionRepository adminSessionRepository;
    private final OperationLogRepository operationLogService;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final TimingAttackMitigation timingAttackMitigation;

    public LoginResponseDto login(@NonNull LoginRequestDto loginDto, String lastIp, String userAgent, String deviceName) {
        final Instant now = Instant.now();

        final Optional<AuthAdminRepository.SelectedAdminUser> adminUserOptional = adminRepository.findByUsernameOrEmail(loginDto.username(), loginDto.email());
        if (adminUserOptional.isEmpty()) {
            passwordEncoder.matches(loginDto.password(), timingAttackMitigation.getDummyPasswordHash());
            throw new UnauthorizedException(ErrorCode.AUTH_INVALID_CREDENTIALS);
        }

        final AuthAdminRepository.SelectedAdminUser adminUser = adminUserOptional.get();
        if (!passwordEncoder.matches(loginDto.password(), adminUser.passwordHash())) {
            throw new UnauthorizedException(ErrorCode.AUTH_INVALID_CREDENTIALS);
        }

        if (adminUser.status() != AdminUserStatus.ACTIVE) {
            throw ForbiddenException.accountDisabled();
        }

        UUID sessionId = UUID.randomUUID();

        final UserJwtPayload userPayload = UserJwtPayload.forAdmin(
                adminUser.id(), adminUser.username(), adminUser.email(), adminUser.role().name(), sessionId
        );

        final String accessToken = jwtTokenService.generateAccessToken(userPayload, now);

        final GeneratedToken generateRefreshToken = jwtTokenService.generateRefreshToken(adminUser.id(), sessionId, now);
        final String refreshTokenHash = HashUtils.sha256Hex(generateRefreshToken.token());

        tx.executeWithoutResult((_) -> {
            // Lock the admin user row to prevent concurrency conflicts with operations like password reset or logout
            // 加锁为了和其它操作列如重置密码、注销互斥避免并发
            adminSessionRepository.lockAdminUserForSessionUpdate(adminUser.id());

            adminSessionRepository.upsert(
                    sessionId,
                    adminUser.id(),
                    loginDto.deviceUuid(),
                    deviceName,
                    lastIp,
                    userAgent,
                    generateRefreshToken.expiresAt(),
                    now,
                    refreshTokenHash
            );

            adminSessionRepository.revokeOldsSessions(
                    adminUser.id(),
                    adminSessionProperties.getMaxSessionByRole(adminUser.role()),
                    now
            );

            adminRepository.setLastLoginAt(adminUser.id(), now);

            operationLogService.insert(AuthAuditEvents.adminLogin(
                    adminUser.id(),
                    adminUser.username(),
                    sessionId,
                    deviceName,
                    lastIp,
                    userAgent,
                    now
            ));
        });

        return new LoginResponseDto(
                accessToken,
                generateRefreshToken.token(),
                userPayload.userId(),
                userPayload.username(),
                userPayload.email(),
                userPayload.userTypes(),
                userPayload.roles(),
                sessionId
        );
    }

    public RefreshTokenResponse refreshToken(String refreshToken, UUID sessionId, String lastIp, String userAgent) {
        final Instant now = Instant.now();
        final String refreshTokenHash = HashUtils.sha256Hex(refreshToken);

        final AuthAdminSessionRepository.SelectedSessionWithUser session = adminSessionRepository
                .getSessionByCurrentOrPreviousRefreshTokenHash(refreshTokenHash)
                .orElseThrow(() -> new UnauthorizedException(ErrorCode.AUTH_REFRESH_TOKEN_INVALID));

        if (session.revokedAt() != null) {
            throw new UnauthorizedException(ErrorCode.AUTH_SESSION_REVOKED);
        }
        if (session.expiresAt().isBefore(now)) {
            throw new UnauthorizedException(ErrorCode.AUTH_SESSION_EXPIRED);
        }
        if (session.status() != AdminUserStatus.ACTIVE) {
            adminSessionRepository.revokeAllByUser(session.adminUserId(), AdminSessionRevocationReason.ACCOUNT_DISABLED, now);
            throw ForbiddenException.accountDisabled();
        }

        if (refreshTokenHash.equals(session.previousRefreshTokenHash())) {
            Duration sinceActive = Duration.between(session.lastActiveAt(), now);

            if (sinceActive.compareTo(Duration.ofSeconds(adminSessionProperties.refreshGraceSeconds())) <= 0) {
                // Frontend needs to wait for the in-flight refresh to finish instead of retrying with the old token.
                throw new ConflictException(ErrorCode.AUTH_REFRESH_IN_PROGRESS);
            }

            // Outside grace window: treat as a stolen token being replayed — revoke everything.
            tx.executeWithoutResult(_ -> {
                int revokedCount = adminSessionRepository.revokeAllByUser(
                        session.adminUserId(), AdminSessionRevocationReason.TOKEN_REUSE_DETECTED, now);

                // Use current request's IP/UA (the replayer), not the stored ones from the last legitimate request.
                operationLogService.insert(AuthAuditEvents.tokenReuseDetected(
                        session.adminUserId(),
                        session.username(),
                        session.sessionId(),
                        session.deviceName(),
                        revokedCount,
                        sinceActive.toSeconds(),
                        lastIp,
                        userAgent,
                        now
                ));
            });
            throw new UnauthorizedException(ErrorCode.AUTH_REFRESH_REUSED);
        }

        final GeneratedToken newToken = jwtTokenService.generateRefreshToken(session.adminUserId(), sessionId, now);
        final String newRefreshTokenHash = HashUtils.sha256Hex(newToken.token());

        // CAS update: only succeeds if no concurrent request already rotated this token.
        final int rows = adminSessionRepository.rotateRefreshToken(
                sessionId, refreshTokenHash, newRefreshTokenHash, lastIp, newToken.expiresAt(), now);
        if (rows == 0) {
            // Another request won the race; avise the frontend to wait for its response.
            throw new ConflictException(ErrorCode.AUTH_REFRESH_IN_PROGRESS);
        }

        final UserJwtPayload payload = UserJwtPayload.forAdmin(
                session.adminUserId(), session.username(), session.email(), session.role().name(), sessionId
        );

        final String newAccessToken = jwtTokenService.generateAccessToken(payload, now);

        return new RefreshTokenResponse(newAccessToken, newToken.token());
    }

    public void logout(UUID sessionId, Long userId, String username, String lastIp, String userAgent) {
        final Instant now = Instant.now();
        tx.executeWithoutResult(_ -> {
            adminSessionRepository.revokeSession(sessionId, AdminSessionRevocationReason.LOGOUT, now);
            operationLogService.insert(AuthAuditEvents.adminLogout(userId, username, sessionId, lastIp, userAgent, now));
        });
    }

}
