package org.dsqrwym.langora.auth.jwt;

import org.dsqrwym.langora.infrastructure.security.config.JwtConfig;
import org.dsqrwym.langora.infrastructure.security.properties.JwtProperties;
import org.dsqrwym.langora.infrastructure.web.exception.UnauthorizedException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class JwtTokenService {
    private final JwtEncoder jwtEncoder;
    private final JwtDecoder refreshTokenDecoder;
    public final JwtProperties jwtProperties;

    public JwtTokenService(
            JwtEncoder jwtEncoder,
             @Qualifier(JwtConfig.REFRESH_TOKEN_DECODER) JwtDecoder refreshTokenDecoder,
            JwtProperties jwtProperties
    ) {
        this.jwtEncoder = jwtEncoder;
        this.refreshTokenDecoder = refreshTokenDecoder;
        this.jwtProperties = jwtProperties;
    }

    public String generateAccessToken(UserJwtPayload payload, Instant now) {
        final Instant expiresAt = now.plus(jwtProperties.accessTokenExpiration());
        final JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();

        final JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(jwtProperties.issuer())
                .issuedAt(now)
                .expiresAt(expiresAt)
                .subject(String.valueOf(payload.userId()))
                .claim(UserJwtPayload.CLAIM_NAME_USER_ID, payload.userId())
                .claim(UserJwtPayload.CLAIM_NAME_USERNAME, payload.username())
                .claim(UserJwtPayload.CLAIM_NAME_EMAIL, payload.email())
                .claim(UserJwtPayload.CLAIM_NAME_USER_TYPE, payload.userTypes().name())
                .claim(UserJwtPayload.CLAIM_NAME_ROLES, payload.roles())
                .claim(UserJwtPayload.CLAIM_NAME_SESSION_ID, payload.sessionId())
                .build();

        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    public GeneratedToken generateRefreshToken(Long userId, UUID sessionId, Instant now) {
        final Instant expiresAt = now.plus(jwtProperties.refreshTokenExpiration());

        return new GeneratedToken(
                jwtEncoder.encode(
                        JwtEncoderParameters.from(
                                JwsHeader.with(MacAlgorithm.HS256).build(),
                                JwtClaimsSet.builder()
                                        .issuer(jwtProperties.issuer())
                                        .issuedAt(now)
                                        .expiresAt(expiresAt)
                                        .subject(String.valueOf(userId))
                                        .claim(UserJwtPayload.CLAIM_NAME_SESSION_ID, sessionId)
                                        .build()
                        )
                ).getTokenValue(),
                expiresAt
        );
    }

    public UUID verifyAndExtractSessionId(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new UnauthorizedException("Missing refresh token");
        }
        try {
            Jwt jwt = refreshTokenDecoder.decode(refreshToken);
            String raw = jwt.getClaimAsString(UserJwtPayload.CLAIM_NAME_SESSION_ID);
            if (raw == null) {
                throw new UnauthorizedException("Refresh token missing session id");
            }
            return UUID.fromString(raw);
        } catch (JwtException | IllegalArgumentException e) {
            throw new UnauthorizedException("Invalid or expired refresh token");
        }
    }
}
