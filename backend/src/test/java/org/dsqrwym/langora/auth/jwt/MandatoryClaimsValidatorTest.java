package org.dsqrwym.langora.auth.jwt;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MandatoryClaimsValidatorTest {

    private final MandatoryClaimsValidator validator = new MandatoryClaimsValidator();

    @Test
    void acceptsCompleteAccessClaims() {
        Jwt token = baseToken()
                .claim(UserJwtPayload.CLAIM_NAME_USER_ID, 7L)
                .claim(UserJwtPayload.CLAIM_NAME_USERNAME, "Admin")
                .claim(UserJwtPayload.CLAIM_NAME_USER_TYPE, "ADMIN")
                .claim(UserJwtPayload.CLAIM_NAME_SESSION_ID, UUID.randomUUID().toString())
                .claim(UserJwtPayload.CLAIM_NAME_ROLES, List.of("ADMIN"))
                .build();

        assertFalse(validator.validate(token).hasErrors());
    }

    @Test
    void rejectsMissingRequiredClaimAndEmptyRoles() {
        Jwt missingUsername = baseToken()
                .claim(UserJwtPayload.CLAIM_NAME_USER_ID, 7L)
                .claim(UserJwtPayload.CLAIM_NAME_USER_TYPE, "ADMIN")
                .claim(UserJwtPayload.CLAIM_NAME_SESSION_ID, UUID.randomUUID().toString())
                .claim(UserJwtPayload.CLAIM_NAME_ROLES, List.of("ADMIN"))
                .build();
        Jwt emptyRoles = baseToken()
                .claim(UserJwtPayload.CLAIM_NAME_USER_ID, 7L)
                .claim(UserJwtPayload.CLAIM_NAME_USERNAME, "Admin")
                .claim(UserJwtPayload.CLAIM_NAME_USER_TYPE, "ADMIN")
                .claim(UserJwtPayload.CLAIM_NAME_SESSION_ID, UUID.randomUUID().toString())
                .claim(UserJwtPayload.CLAIM_NAME_ROLES, List.of())
                .build();

        assertTrue(validator.validate(missingUsername).hasErrors());
        assertTrue(validator.validate(emptyRoles).hasErrors());
    }

    private static Jwt.Builder baseToken() {
        Instant now = Instant.now();
        return Jwt.withTokenValue("token")
                .header("alg", "HS256")
                .subject("7")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(60));
    }
}
