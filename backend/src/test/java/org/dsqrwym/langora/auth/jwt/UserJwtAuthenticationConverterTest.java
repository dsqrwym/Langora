package org.dsqrwym.langora.auth.jwt;

import org.dsqrwym.langora.auth.UserTypes;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UserJwtAuthenticationConverterTest {

    private final UserJwtAuthenticationConverter converter = new UserJwtAuthenticationConverter();

    @Test
    void convertsCompleteJwtToTypedPrincipalAndAuthorities() {
        UUID sessionId = UUID.randomUUID();
        UserJwtAuthenticationToken authentication = converter.convert(validToken(sessionId));

        assertEquals(7L, authentication.getPrincipal().userId());
        assertEquals("Admin", authentication.getPrincipal().username());
        assertEquals(sessionId, authentication.getPrincipal().sessionId());
        assertTrueAuthority(authentication, "TYPE_ADMIN");
        assertTrueAuthority(authentication, "ROLE_ADMIN");
    }

    @Test
    void rejectsInvalidRoleAndSessionClaims() {
        Jwt invalidType = tokenBuilder(UUID.randomUUID())
                .claim(UserJwtPayload.CLAIM_NAME_USER_TYPE, "UNKNOWN")
                .build();
        Jwt invalidSession = tokenBuilder(UUID.randomUUID())
                .claim(UserJwtPayload.CLAIM_NAME_SESSION_ID, "not-a-uuid")
                .build();

        assertThrows(BadCredentialsException.class, () -> converter.convert(invalidType));
        assertThrows(BadCredentialsException.class, () -> converter.convert(invalidSession));
    }

    @Test
    void rejectsMissingEmailAndEmptyRoles() {
        Jwt missingEmail = Jwt.withTokenValue("token")
                .header("alg", "HS256")
                .subject("7")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .claim(UserJwtPayload.CLAIM_NAME_USER_ID, 7L)
                .claim(UserJwtPayload.CLAIM_NAME_USERNAME, "Admin")
                .claim(UserJwtPayload.CLAIM_NAME_USER_TYPE, UserTypes.ADMIN.name())
                .claim(UserJwtPayload.CLAIM_NAME_ROLES, List.of("ADMIN"))
                .claim(UserJwtPayload.CLAIM_NAME_SESSION_ID, UUID.randomUUID().toString())
                .build();
        Jwt emptyRoles = tokenBuilder(UUID.randomUUID())
                .claim(UserJwtPayload.CLAIM_NAME_ROLES, List.of())
                .build();

        assertThrows(BadCredentialsException.class, () -> converter.convert(missingEmail));
        assertThrows(BadCredentialsException.class, () -> converter.convert(emptyRoles));
    }

    private static Jwt validToken(UUID sessionId) {
        return tokenBuilder(sessionId).build();
    }

    private static Jwt.Builder tokenBuilder(UUID sessionId) {
        Instant now = Instant.now();
        return Jwt.withTokenValue("token")
                .header("alg", "HS256")
                .subject("7")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(60))
                .claim(UserJwtPayload.CLAIM_NAME_USER_ID, 7L)
                .claim(UserJwtPayload.CLAIM_NAME_USERNAME, "Admin")
                .claim(UserJwtPayload.CLAIM_NAME_EMAIL, "admin@langora.com")
                .claim(UserJwtPayload.CLAIM_NAME_USER_TYPE, UserTypes.ADMIN.name())
                .claim(UserJwtPayload.CLAIM_NAME_ROLES, List.of("ADMIN"))
                .claim(UserJwtPayload.CLAIM_NAME_SESSION_ID, sessionId.toString());
    }

    private static void assertTrueAuthority(UserJwtAuthenticationToken authentication, String authority) {
        boolean present = authentication.getAuthorities().stream()
                .anyMatch(item -> item.getAuthority().equals(authority));
        if (!present) {
            throw new AssertionError("Expected authority " + authority);
        }
    }
}
