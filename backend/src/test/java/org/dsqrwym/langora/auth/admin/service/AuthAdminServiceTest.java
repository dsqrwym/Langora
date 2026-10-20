package org.dsqrwym.langora.auth.admin.service;

import org.dsqrwym.langora.audit.OperationLogRepository;
import org.dsqrwym.langora.auth.admin.config.AuthAdminSessionProperties;
import org.dsqrwym.langora.auth.admin.repository.AuthAdminRepository;
import org.dsqrwym.langora.auth.admin.repository.AuthAdminSessionRepository;
import org.dsqrwym.langora.auth.dto.LoginRequestDto;
import org.dsqrwym.langora.auth.dto.LoginResponseDto;
import org.dsqrwym.langora.auth.dto.RefreshTokenResponse;
import org.dsqrwym.langora.auth.jwt.GeneratedToken;
import org.dsqrwym.langora.auth.jwt.JwtTokenService;
import org.dsqrwym.langora.infrastructure.jooq.enums.AdminSessionRevocationReason;
import org.dsqrwym.langora.infrastructure.jooq.enums.AdminUserRole;
import org.dsqrwym.langora.infrastructure.jooq.enums.AdminUserStatus;
import org.dsqrwym.langora.infrastructure.security.TimingAttackMitigation;
import org.dsqrwym.langora.infrastructure.util.HashUtils;
import org.dsqrwym.langora.infrastructure.web.ErrorCode;
import org.dsqrwym.langora.infrastructure.web.exception.ConflictException;
import org.dsqrwym.langora.infrastructure.web.exception.ForbiddenException;
import org.dsqrwym.langora.infrastructure.web.exception.UnauthorizedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class AuthAdminServiceTest {

    private static final long USER_ID = 7L;
    private static final UUID SESSION_ID = UUID.fromString("00000000-0000-0000-0000-000000000007");

    @Mock
    private TransactionTemplate transactionTemplate;
    @Mock
    private AuthAdminRepository adminRepository;
    @Mock
    private AuthAdminSessionRepository sessionRepository;
    @Mock
    private OperationLogRepository operationLogRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtTokenService jwtTokenService;
    @Mock
    private TimingAttackMitigation timingAttackMitigation;

    private AuthAdminService service;

    @BeforeEach
    void setUp() {
        service = new AuthAdminService(
                transactionTemplate,
                new AuthAdminSessionProperties(3, 5, 5, 5, new AuthAdminSessionProperties.Cleanup(true, Duration.ofDays(365))),
                adminRepository,
                sessionRepository,
                operationLogRepository,
                passwordEncoder,
                jwtTokenService,
                timingAttackMitigation
        );

    }

    @Test
    void loginUsesDummyBcryptComparisonWhenUserDoesNotExist() {
        LoginRequestDto request = loginRequest();
        given(adminRepository.findByUsernameOrEmail(request.username(), request.email())).willReturn(Optional.empty());
        given(timingAttackMitigation.getDummyPasswordHash()).willReturn("dummy-hash");

        UnauthorizedException exception = assertThrows(UnauthorizedException.class,
                () -> service.login(request, "127.0.0.1", "ua", "device"));

        assertEquals(ErrorCode.AUTH_INVALID_CREDENTIALS, exception.code);
        then(passwordEncoder).should().matches("password", "dummy-hash");
        then(sessionRepository).shouldHaveNoInteractions();
        then(operationLogRepository).shouldHaveNoInteractions();
    }

    @Test
    void loginRejectsWrongPasswordWithoutCreatingSession() {
        LoginRequestDto request = loginRequest();
        given(adminRepository.findByUsernameOrEmail(request.username(), request.email())).willReturn(Optional.of(activeUser()));
        given(passwordEncoder.matches("password", "stored-hash")).willReturn(false);

        UnauthorizedException exception = assertThrows(UnauthorizedException.class,
                () -> service.login(request, "127.0.0.1", "ua", "device"));

        assertEquals(ErrorCode.AUTH_INVALID_CREDENTIALS, exception.code);
        then(sessionRepository).shouldHaveNoInteractions();
        then(operationLogRepository).shouldHaveNoInteractions();
    }

    @Test
    void loginRejectsDisabledAccountWithoutCreatingSession() {
        LoginRequestDto request = loginRequest();
        given(adminRepository.findByUsernameOrEmail(request.username(), request.email())).willReturn(Optional.of(disabledUser()));
        given(passwordEncoder.matches("password", "stored-hash")).willReturn(true);

        ForbiddenException exception = assertThrows(ForbiddenException.class,
                () -> service.login(request, "127.0.0.1", "ua", "device"));

        assertEquals(ErrorCode.AUTH_ACCOUNT_DISABLED, exception.code);
        then(sessionRepository).shouldHaveNoInteractions();
    }

    @Test
    void loginCreatesSessionUpdatesLoginTimeAndWritesAuditEvent() {
        executeTransactionCallbacks();
        LoginRequestDto request = loginRequest();
        given(adminRepository.findByUsernameOrEmail(request.username(), request.email())).willReturn(Optional.of(activeUser()));
        given(passwordEncoder.matches("password", "stored-hash")).willReturn(true);
        given(jwtTokenService.generateAccessToken(any(), any())).willReturn("access-token");
        given(jwtTokenService.generateRefreshToken(eq(USER_ID), any(UUID.class), any(Instant.class)))
                .willAnswer(invocation -> {
                    Instant issuedAt = invocation.getArgument(2);
                    return new GeneratedToken("refresh-token", issuedAt.plus(Duration.ofDays(1)));
                });

        LoginResponseDto response = service.login(request, "127.0.0.1", "ua", "Windows . Chrome");

        assertEquals("access-token", response.getAccessToken());
        assertEquals("refresh-token", response.getRefreshToken());
        assertEquals(USER_ID, response.getUserId());

        ArgumentCaptor<UUID> sessionId = ArgumentCaptor.forClass(UUID.class);
        then(sessionRepository).should().lockAdminUserForSessionUpdate(USER_ID);
        then(sessionRepository).should().upsert(
                sessionId.capture(), eq(USER_ID), eq("device-1"), eq("Windows . Chrome"),
                eq("127.0.0.1"), eq("ua"), any(Instant.class), any(Instant.class),
                eq(HashUtils.sha256Hex("refresh-token"))
        );
        assertEquals(sessionId.getValue(), response.getSessionId());
        then(sessionRepository).should().revokeOldsSessions(eq(USER_ID), eq(5), any(Instant.class));
        then(adminRepository).should().setLastLoginAt(eq(USER_ID), any(Instant.class));
        then(operationLogRepository).should().insert(any());
    }

    @Test
    void refreshRejectsUnknownTokenHash() {
        String token = "unknown-refresh";
        given(sessionRepository.getSessionByCurrentOrPreviousRefreshTokenHash(HashUtils.sha256Hex(token)))
                .willReturn(Optional.empty());

        UnauthorizedException exception = assertThrows(UnauthorizedException.class,
                () -> service.refreshToken(token, SESSION_ID, "127.0.0.1", "ua"));

        assertEquals(ErrorCode.AUTH_REFRESH_TOKEN_INVALID, exception.code);
    }

    @Test
    void refreshRejectsRevokedAndExpiredSession() {
        String token = "refresh";
        String hash = HashUtils.sha256Hex(token);
        given(sessionRepository.getSessionByCurrentOrPreviousRefreshTokenHash(hash))
                .willReturn(Optional.of(session(hash, null, Instant.now(), Instant.now().plusSeconds(60), Instant.now())));

        UnauthorizedException revoked = assertThrows(UnauthorizedException.class,
                () -> service.refreshToken(token, SESSION_ID, "127.0.0.1", "ua"));
        assertEquals(ErrorCode.AUTH_SESSION_REVOKED, revoked.code);

        given(sessionRepository.getSessionByCurrentOrPreviousRefreshTokenHash(hash))
                .willReturn(Optional.of(session(hash, null, null, Instant.now().minusSeconds(60), Instant.now())));
        UnauthorizedException expired = assertThrows(UnauthorizedException.class,
                () -> service.refreshToken(token, SESSION_ID, "127.0.0.1", "ua"));
        assertEquals(ErrorCode.AUTH_SESSION_EXPIRED, expired.code);
    }

    @Test
    void refreshRevokesDisabledAccountsSessions() {
        String token = "refresh";
        String hash = HashUtils.sha256Hex(token);
        AuthAdminSessionRepository.SelectedSessionWithUser disabled = new AuthAdminSessionRepository.SelectedSessionWithUser(
                SESSION_ID, USER_ID, hash, null, null, Instant.now().plusSeconds(60), Instant.now(),
                "device", "127.0.0.1", "ua", "Admin", "admin@langora.com",
                AdminUserRole.ADMIN, AdminUserStatus.DISABLED
        );
        given(sessionRepository.getSessionByCurrentOrPreviousRefreshTokenHash(hash)).willReturn(Optional.of(disabled));

        ForbiddenException exception = assertThrows(ForbiddenException.class,
                () -> service.refreshToken(token, SESSION_ID, "127.0.0.1", "ua"));

        assertEquals(ErrorCode.AUTH_ACCOUNT_DISABLED, exception.code);
        then(sessionRepository).should().revokeAllByUser(eq(USER_ID), eq(AdminSessionRevocationReason.ACCOUNT_DISABLED), any(Instant.class));
    }

    @Test
    void refreshReportsInProgressWhenPreviousTokenIsInsideGracePeriod() {
        String oldToken = "old-refresh";
        String oldHash = HashUtils.sha256Hex(oldToken);
        given(sessionRepository.getSessionByCurrentOrPreviousRefreshTokenHash(oldHash))
                .willReturn(Optional.of(session("current-hash", oldHash, null,
                        Instant.now().plusSeconds(60), Instant.now())));

        ConflictException exception = assertThrows(ConflictException.class,
                () -> service.refreshToken(oldToken, SESSION_ID, "127.0.0.1", "ua"));

        assertEquals(ErrorCode.AUTH_REFRESH_IN_PROGRESS, exception.code);
        then(sessionRepository).should(never()).revokeAllByUser(
                eq(USER_ID), eq(AdminSessionRevocationReason.TOKEN_REUSE_DETECTED), any(Instant.class));
    }

    @Test
    void refreshRevokesAllSessionsWhenPreviousTokenIsReplayedAfterGracePeriod() {
        executeTransactionCallbacks();
        String oldToken = "old-refresh";
        String oldHash = HashUtils.sha256Hex(oldToken);
        given(sessionRepository.getSessionByCurrentOrPreviousRefreshTokenHash(oldHash))
                .willReturn(Optional.of(session("current-hash", oldHash, null,
                        Instant.now().plusSeconds(60), Instant.now().minusSeconds(30))));
        given(sessionRepository.revokeAllByUser(eq(USER_ID), eq(AdminSessionRevocationReason.TOKEN_REUSE_DETECTED), any(Instant.class)))
                .willReturn(2);

        UnauthorizedException exception = assertThrows(UnauthorizedException.class,
                () -> service.refreshToken(oldToken, SESSION_ID, "127.0.0.2", "replayer-agent"));

        assertEquals(ErrorCode.AUTH_REFRESH_REUSED, exception.code);
        then(sessionRepository).should().revokeAllByUser(
                eq(USER_ID), eq(AdminSessionRevocationReason.TOKEN_REUSE_DETECTED), any(Instant.class));
        then(operationLogRepository).should().insert(any());
        then(jwtTokenService).shouldHaveNoInteractions();
    }

    @Test
    void refreshRotatesCurrentTokenWhenCompareAndSetSucceeds() {
        String oldToken = "current-refresh";
        String oldHash = HashUtils.sha256Hex(oldToken);
        given(sessionRepository.getSessionByCurrentOrPreviousRefreshTokenHash(oldHash))
                .willReturn(Optional.of(session(oldHash, null, null, Instant.now().plusSeconds(60), Instant.now())));
        given(jwtTokenService.generateRefreshToken(eq(USER_ID), eq(SESSION_ID), any(Instant.class)))
                .willAnswer(invocation -> new GeneratedToken("new-refresh", invocation.<Instant>getArgument(2).plus(Duration.ofDays(1))));
        given(sessionRepository.rotateRefreshToken(eq(SESSION_ID), eq(oldHash), eq(HashUtils.sha256Hex("new-refresh")),
                eq("127.0.0.1"), any(Instant.class), any(Instant.class))).willReturn(1);
        given(jwtTokenService.generateAccessToken(any(), any())).willReturn("new-access");

        RefreshTokenResponse response = service.refreshToken(oldToken, SESSION_ID, "127.0.0.1", "ua");

        assertEquals("new-access", response.getAccessToken());
        assertEquals("new-refresh", response.getRefreshToken());
        then(sessionRepository).should().rotateRefreshToken(eq(SESSION_ID), eq(oldHash),
                eq(HashUtils.sha256Hex("new-refresh")), eq("127.0.0.1"), any(Instant.class), any(Instant.class));
    }

    @Test
    void refreshReportsInProgressWhenCompareAndSetLosesRace() {
        String oldToken = "current-refresh";
        String oldHash = HashUtils.sha256Hex(oldToken);
        given(sessionRepository.getSessionByCurrentOrPreviousRefreshTokenHash(oldHash))
                .willReturn(Optional.of(session(oldHash, null, null, Instant.now().plusSeconds(60), Instant.now())));
        given(jwtTokenService.generateRefreshToken(eq(USER_ID), eq(SESSION_ID), any(Instant.class)))
                .willAnswer(invocation -> new GeneratedToken("new-refresh", invocation.<Instant>getArgument(2).plus(Duration.ofDays(1))));
        given(sessionRepository.rotateRefreshToken(eq(SESSION_ID), eq(oldHash), eq(HashUtils.sha256Hex("new-refresh")),
                anyString(), any(Instant.class), any(Instant.class))).willReturn(0);

        ConflictException exception = assertThrows(ConflictException.class,
                () -> service.refreshToken(oldToken, SESSION_ID, "127.0.0.1", "ua"));

        assertEquals(ErrorCode.AUTH_REFRESH_IN_PROGRESS, exception.code);
    }

    @Test
    void logoutRevokesOnlyCurrentSessionAndWritesAuditEvent() {
        executeTransactionCallbacks();
        service.logout(SESSION_ID, USER_ID, "Admin", "127.0.0.1", "ua");

        then(sessionRepository).should().revokeSession(
                eq(SESSION_ID), eq(AdminSessionRevocationReason.LOGOUT), any(Instant.class));
        then(operationLogRepository).should().insert(any());
    }

    private static LoginRequestDto loginRequest() {
        return new LoginRequestDto(null, "Admin", "password", "device-1");
    }

    private void executeTransactionCallbacks() {
        doAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            Consumer<TransactionStatus> work = invocation.getArgument(0);
            work.accept(mock(TransactionStatus.class));
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());
    }

    private static AuthAdminRepository.SelectedAdminUser activeUser() {
        return new AuthAdminRepository.SelectedAdminUser(
                USER_ID, "Admin", "admin@langora.com", AdminUserRole.ADMIN, "stored-hash", AdminUserStatus.ACTIVE);
    }

    private static AuthAdminRepository.SelectedAdminUser disabledUser() {
        return new AuthAdminRepository.SelectedAdminUser(
                USER_ID, "Admin", "admin@langora.com", AdminUserRole.ADMIN, "stored-hash", AdminUserStatus.DISABLED);
    }

    private static AuthAdminSessionRepository.SelectedSessionWithUser session(
            String currentHash, String previousHash, Instant revokedAt, Instant expiresAt, Instant lastActiveAt) {
        return new AuthAdminSessionRepository.SelectedSessionWithUser(
                SESSION_ID, USER_ID, currentHash, previousHash, revokedAt, expiresAt, lastActiveAt,
                "device", "127.0.0.1", "ua", "Admin", "admin@langora.com",
                AdminUserRole.ADMIN, AdminUserStatus.ACTIVE
        );
    }
}
