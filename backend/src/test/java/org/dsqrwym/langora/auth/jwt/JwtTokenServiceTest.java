package org.dsqrwym.langora.auth.jwt;

import org.dsqrwym.langora.infrastructure.security.config.JwtConfig;
import org.dsqrwym.langora.infrastructure.security.properties.JwtProperties;
import org.dsqrwym.langora.infrastructure.web.ErrorCode;
import org.dsqrwym.langora.infrastructure.web.exception.UnauthorizedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtTokenServiceTest {

    private JwtTokenService service;
    private JwtDecoder accessTokenDecoder;

    @BeforeEach
    void setUp() {
        JwtProperties properties = new JwtProperties(
                "test-only-jwt-secret-key-must-be-at-least-32-chars",
                "langora-test",
                Duration.ofMinutes(15),
                Duration.ofDays(1)
        );
        JwtConfig config = new JwtConfig();
        SecretKey key = config.jwtSecretKey(properties);
        service = new JwtTokenService(config.jwtEncoder(key), config.refreshTokenDecoder(key), properties);
        accessTokenDecoder = config.accessTokenDecoder(key);
    }

    @Test
    void accessTokenContainsRequiredAuthenticatedPrincipalClaims() {
        UUID sessionId = UUID.randomUUID();
        UserJwtPayload payload = new UserJwtPayload(
                42L, "Admin", "admin@langora.com", org.dsqrwym.langora.auth.UserTypes.ADMIN,
                List.of("ADMIN"), sessionId
        );

        String token = service.generateAccessToken(payload, Instant.now());
        Jwt decoded = accessTokenDecoder.decode(token);

        assertEquals(42L, ((Number) decoded.getClaim(UserJwtPayload.CLAIM_NAME_USER_ID)).longValue());
        assertEquals("Admin", decoded.getClaimAsString(UserJwtPayload.CLAIM_NAME_USERNAME));
        assertEquals("admin@langora.com", decoded.getClaimAsString(UserJwtPayload.CLAIM_NAME_EMAIL));
        assertEquals("ADMIN", decoded.getClaimAsString(UserJwtPayload.CLAIM_NAME_USER_TYPE));
        assertEquals(List.of("ADMIN"), decoded.getClaimAsStringList(UserJwtPayload.CLAIM_NAME_ROLES));
        assertEquals(sessionId.toString(), decoded.getClaimAsString(UserJwtPayload.CLAIM_NAME_SESSION_ID));
    }

    @Test
    void refreshTokenReturnsOriginalSessionId() {
        UUID sessionId = UUID.randomUUID();
        GeneratedToken token = service.generateRefreshToken(42L, sessionId, Instant.now());

        assertEquals(sessionId, service.verifyAndExtractSessionId(token.token()));
    }

    @Test
    void missingRefreshTokenHasDedicatedErrorCode() {
        UnauthorizedException exception = assertThrows(
                UnauthorizedException.class,
                () -> service.verifyAndExtractSessionId(" ")
        );

        assertEquals(ErrorCode.AUTH_REFRESH_TOKEN_MISSING, exception.code);
    }

    @Test
    void malformedAndExpiredRefreshTokensAreRejected() {
        UnauthorizedException malformed = assertThrows(
                UnauthorizedException.class,
                () -> service.verifyAndExtractSessionId("not-a-jwt")
        );
        assertEquals(ErrorCode.AUTH_REFRESH_TOKEN_INVALID, malformed.code);

        GeneratedToken expired = service.generateRefreshToken(
                42L, UUID.randomUUID(), Instant.now().minus(Duration.ofDays(2)));
        UnauthorizedException expiredException = assertThrows(
                UnauthorizedException.class,
                () -> service.verifyAndExtractSessionId(expired.token())
        );
        assertEquals(ErrorCode.AUTH_REFRESH_TOKEN_INVALID, expiredException.code);
    }

    @Test
    void accessDecoderRejectsRefreshTokenBecauseItLacksAccessClaims() {
        GeneratedToken refresh = service.generateRefreshToken(42L, UUID.randomUUID(), Instant.now());

        assertThrows(JwtException.class, () -> accessTokenDecoder.decode(refresh.token()));
    }
}
