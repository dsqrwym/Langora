package org.dsqrwym.langora.auth.service;

import org.dsqrwym.langora.infrastructure.security.properties.CsrfProperties;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthCsrfServiceTest {

    private final AuthCsrfService service = new AuthCsrfService(
            new CsrfProperties("test-only-csrf-secret-key-must-be-at-least-32-chars"));

    @Test
    void generatesStableTokenForSameSession() {
        UUID sessionId = UUID.randomUUID();

        assertEquals(service.generateCsrfToken(sessionId), service.generateCsrfToken(sessionId));
    }

    @Test
    void acceptsTokenBoundToItsSessionOnly() {
        UUID sessionId = UUID.randomUUID();
        String token = service.generateCsrfToken(sessionId);

        assertTrue(service.validateCsrfToken(sessionId, token));
        assertFalse(service.validateCsrfToken(UUID.randomUUID(), token));
    }

    @Test
    void rejectsBlankAndTamperedTokens() {
        UUID sessionId = UUID.randomUUID();
        String token = service.generateCsrfToken(sessionId);

        assertFalse(service.validateCsrfToken(sessionId, null));
        assertFalse(service.validateCsrfToken(sessionId, " "));
        assertFalse(service.validateCsrfToken(sessionId, token.substring(0, token.length() - 1) + "0"));
    }
}
