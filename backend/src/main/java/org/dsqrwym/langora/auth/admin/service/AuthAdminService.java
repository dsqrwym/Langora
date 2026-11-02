package org.dsqrwym.langora.auth.admin.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dsqrwym.langora.audit.OperationLogRepository;
import org.dsqrwym.langora.audit.auth.AuthAuditEvents;
import org.dsqrwym.langora.auth.UserTypes;
import org.dsqrwym.langora.auth.admin.config.AuthAdminSessionProperties;
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
import org.dsqrwym.langora.infrastructure.security.captcha.verifier.CaptchaVerifier;
import org.dsqrwym.langora.infrastructure.security.login.LoginAttemptAccountKey;
import org.dsqrwym.langora.infrastructure.security.login.LoginAttemptService;
import org.dsqrwym.langora.infrastructure.util.HashUtils;
import org.dsqrwym.langora.infrastructure.web.ErrorCode;
import org.dsqrwym.langora.infrastructure.web.exception.CaptchaException;
import org.dsqrwym.langora.infrastructure.web.exception.ConflictException;
import org.dsqrwym.langora.infrastructure.web.exception.ForbiddenException;
import org.dsqrwym.langora.infrastructure.web.exception.UnauthorizedException;
import org.jspecify.annotations.NonNull;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthAdminService {
    private final TransactionTemplate tx;
    private final AuthAdminSessionProperties authAdminSessionProperties;
    private final AuthAdminRepository adminRepository;
    private final AuthAdminSessionRepository adminSessionRepository;
    private final OperationLogRepository operationLogService;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final TimingAttackMitigation timingAttackMitigation;
    private final LoginAttemptService loginAttemptService;
    private final CaptchaVerifier captchaVerifier;

    public LoginResponseDto login(@NonNull LoginRequestDto loginDto, String lastIp, String userAgent, String deviceName) {
        final Instant now = Instant.now();

        final boolean captchaRequired = loginAttemptService.checkIp(lastIp) && captchaVerifier.enabled();

        if (captchaRequired && !StringUtils.hasText(loginDto.captchaToken())) {
            throw CaptchaException.required();
        }

        final Optional<AuthAdminRepository.SelectedAdminUser> found = adminRepository.findByUsernameOrEmail(loginDto.username(), loginDto.email());

        found.ifPresent(user -> loginAttemptService.checkAccount(adminAccountKey(user.id())));

        if (captchaRequired) {
            // catcha 在 bcrypt 前避免消耗
            captchaVerifier.verify(loginDto.captchaToken(), lastIp);
        }

        final AuthAdminRepository.SelectedAdminUser adminUserSnapshot = found.orElseGet(() -> {
            // if isEmpty() 如果查询结果为空
            passwordEncoder.matches(loginDto.password(), timingAttackMitigation.getDummyPasswordHash());
            // 未查询到记
            loginAttemptService.recordIpFailure(lastIp);
            throw new UnauthorizedException(ErrorCode.AUTH_INVALID_CREDENTIALS);
        });

        if (!passwordEncoder.matches(loginDto.password(), adminUserSnapshot.passwordHash())) {
            // 密码错两个都记
            loginAttemptService.recordAccountFailure(adminAccountKey(adminUserSnapshot.id()));
            loginAttemptService.recordIpFailure(lastIp);
            throw new UnauthorizedException(ErrorCode.AUTH_INVALID_CREDENTIALS);
        }

        if (adminUserSnapshot.status() != AdminUserStatus.ACTIVE) {
            throw ForbiddenException.accountDisabled();
        }

        UUID sessionId = UUID.randomUUID();

        final UserJwtPayload userPayload = UserJwtPayload.forAdmin(
                adminUserSnapshot.id(), adminUserSnapshot.username(), adminUserSnapshot.email(), adminUserSnapshot.role(), sessionId
        );

        final String accessToken = jwtTokenService.generateAccessToken(userPayload, now);

        final GeneratedToken generateRefreshToken = jwtTokenService.generateRefreshToken(adminUserSnapshot.id(), sessionId, now);
        final String refreshTokenHash = HashUtils.sha256Hex(generateRefreshToken.token());

        tx.executeWithoutResult(ignored -> {
            // Lock the admin user row to prevent concurrency conflicts with operations like password reset or logout
            // 加锁为了和其它操作列如重置密码、注销互斥避免并发
            final AuthAdminRepository.LockedAdminAuthState userStatus = adminRepository
                    .lockAndGetAdminAuthState(adminUserSnapshot.id())
                    .orElseThrow(() -> new UnauthorizedException(ErrorCode.AUTH_INVALID_CREDENTIALS));

            // second validate to promise user status is not changed
            // 二次校验，确保加锁后的用户状态/密码/角色没有被并发修改
            if (userStatus.status() != AdminUserStatus.ACTIVE
                    || !userStatus.passwordHash().equals(adminUserSnapshot.passwordHash())
                    || userStatus.role() != adminUserSnapshot.role()
            ) {
                throw new UnauthorizedException(ErrorCode.AUTH_INVALID_CREDENTIALS);
            }

            adminSessionRepository.upsert(
                    sessionId,
                    adminUserSnapshot.id(),
                    loginDto.deviceUuid(),
                    deviceName,
                    lastIp,
                    userAgent,
                    generateRefreshToken.expiresAt(),
                    now,
                    refreshTokenHash
            );

            adminSessionRepository.revokeOldsSessions(
                    adminUserSnapshot.id(),
                    authAdminSessionProperties.getMaxSessionByRole(adminUserSnapshot.role()),
                    now
            );

            adminRepository.setLastLoginAt(adminUserSnapshot.id(), now);

            operationLogService.insert(AuthAuditEvents.adminLogin(
                    adminUserSnapshot.id(),
                    adminUserSnapshot.username(),
                    sessionId,
                    deviceName,
                    lastIp,
                    userAgent,
                    now
            ));
        });

        // 登录成功，清除登录失败记录
        loginAttemptService.clearAccount(adminAccountKey(adminUserSnapshot.id()));

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

    public RefreshTokenResponse refreshToken(String refreshToken, String lastIp, String userAgent) {
        final Instant now = Instant.now();
        final String refreshTokenHash = HashUtils.sha256Hex(refreshToken);

        final AuthAdminSessionRepository.SelectedSessionWithUser session = adminSessionRepository
                // refreshToken must be a previous refresh token hash or current refresh token hash in session table
                // refreshToken 必须匹配会话表中的当前哈希或上一个哈希
                .getSessionByCurrentOrPreviousRefreshTokenHash(refreshTokenHash)
                .orElseThrow(() -> new UnauthorizedException(ErrorCode.AUTH_REFRESH_TOKEN_INVALID));

        if (session.revokedAt() != null) {
            throw new UnauthorizedException(ErrorCode.AUTH_SESSION_REVOKED);
        }
        if (session.expiresAt().isBefore(now)) {
            throw new UnauthorizedException(ErrorCode.AUTH_SESSION_EXPIRED);
        }
        if (session.status() != AdminUserStatus.ACTIVE) {
            tx.executeWithoutResult(ignored -> revokeAllSessions(
                    session,
                    AdminSessionRevocationReason.ACCOUNT_DISABLED,
                    null,
                    lastIp,
                    userAgent,
                    now
            ));
            throw ForbiddenException.accountDisabled();
        }

        if (refreshTokenHash.equals(session.previousRefreshTokenHash())) {
            Duration sinceActive = Duration.between(session.lastActiveAt(), now);

            if (sinceActive.compareTo(Duration.ofSeconds(authAdminSessionProperties.refreshGraceSeconds())) <= 0) {
                // Frontend needs to wait for the in-flight refresh to finish instead of retrying with the old token.
                // 前端应等待进行中的刷新完成，而不是用旧 token 重试
                throw new ConflictException(ErrorCode.AUTH_REFRESH_IN_PROGRESS);
            }

            // Outside grace window: treat as a stolen token being replayed — revoke everything.
            // 超出宽限期：视为被盗 token 重放，撤销该账号的全部会话
            tx.executeWithoutResult(ignored -> revokeAllSessions(
                    session,
                    AdminSessionRevocationReason.TOKEN_REUSE_DETECTED,
                    sinceActive.toSeconds(),
                    lastIp,
                    userAgent,
                    now
            ));
            throw new UnauthorizedException(ErrorCode.AUTH_REFRESH_REUSED);
        }

        final GeneratedToken newToken = jwtTokenService.generateRefreshToken(session.adminUserId(), session.sessionId(), now);
        final String newRefreshTokenHash = HashUtils.sha256Hex(newToken.token());

        // CAS update: only succeeds if no concurrent request already rotated this token.
        // CAS 更新：只有没有并发请求已经轮换过该 token 时才会成功
        final int rows = adminSessionRepository.rotateRefreshToken(
                session.sessionId(), refreshTokenHash, newRefreshTokenHash, lastIp, newToken.expiresAt(), now);
        if (rows == 0) {
            // Another request won the race; avise the frontend to wait for its response.
            // 另一个请求赢得了竞争；通知前端等待它的响应
            final var current = adminSessionRepository.getSessionByCurrentOrPreviousRefreshTokenHash(refreshTokenHash)
                    .orElseThrow(() -> new UnauthorizedException(ErrorCode.AUTH_REFRESH_TOKEN_INVALID));
            if (current.revokedAt() != null) {
                throw new UnauthorizedException(ErrorCode.AUTH_SESSION_REVOKED);
            }
            if (current.status() != AdminUserStatus.ACTIVE) {
                throw ForbiddenException.accountDisabled();
            }
            throw new ConflictException(ErrorCode.AUTH_REFRESH_IN_PROGRESS);
        }

        final UserJwtPayload payload = UserJwtPayload.forAdmin(
                session.adminUserId(), session.username(), session.email(), session.role(), session.sessionId()
        );

        final String newAccessToken = jwtTokenService.generateAccessToken(payload, now);

        return new RefreshTokenResponse(newAccessToken, newToken.token());
    }

    public void logout(UUID sessionId, Long userId, String username, String lastIp, String userAgent) {
        final Instant now = Instant.now();
        tx.executeWithoutResult(ignored -> {
            // Lock order: admin_users -> admin_user_sessions -> operation_logs.
            adminRepository.lockAndGetAdminAuthState(userId);
            adminSessionRepository.revokeSession(sessionId, AdminSessionRevocationReason.LOGOUT, now);
            operationLogService.insert(AuthAuditEvents.adminLogout(userId, username, sessionId, lastIp, userAgent, now));
        });
    }

    /**
     * Revokes every active session of the account and records the security event.
     * <p>
     * MUST be called inside a transaction: the account lock, the revocation and
     * the audit row have to commit or roll back together. The account lock also
     * makes "revoke all" deterministic with respect to a concurrent login.
     * <p>
     * 撤销该账号的全部活跃会话，并写入审计事件。
     * 必须在事务内调用：账号锁、撤销和审计必须一起提交或一起回滚；
     * 账号锁也让“撤销全部”与并发登录之间有确定的结果。
     */
    private void revokeAllSessions(
            AuthAdminSessionRepository.SelectedSessionWithUser session,
            AdminSessionRevocationReason reason,
            Long secondsSinceLastActive,
            String lastIp,
            String userAgent,
            Instant now
    ) {
        adminRepository.lockAndGetAdminAuthState(session.adminUserId());

        final int revokedCount = adminSessionRepository.revokeAllByUser(session.adminUserId(), reason, now);

        operationLogService.insert(AuthAuditEvents.sessionsRevoked(
                session.adminUserId(),
                session.username(),
                session.sessionId(),
                session.deviceName(),
                reason,
                revokedCount,
                secondsSinceLastActive,
                lastIp,
                userAgent,
                now
        ));
    }

    /**
     * Admin login-attempt namespace. The key type is {@link UserTypes#ADMIN}; future identity types
     * (for example teachers) pass their own enum value, so counters cannot collide.
     */
    private static LoginAttemptAccountKey<UserTypes> adminAccountKey(Long userId) {
        return LoginAttemptAccountKey.of(UserTypes.ADMIN, Long.toString(userId));
    }

}
