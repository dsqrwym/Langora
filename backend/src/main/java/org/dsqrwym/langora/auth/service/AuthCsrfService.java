package org.dsqrwym.langora.auth.service;

import lombok.RequiredArgsConstructor;
import org.dsqrwym.langora.infrastructure.security.properties.CsrfProperties;
import org.dsqrwym.langora.infrastructure.util.HashUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthCsrfService {
    public static final String CSRF_HEADER_NAME = "X-XSRF-TOKEN";
    private final CsrfProperties csrfProperties;

    /**
     * Generates a CSRF token bound to the given session id.
     */
    public String generateCsrfToken(UUID sessionId) {
        return HashUtils.hmacSha256Hex(csrfProperties.secretKey().getBytes(StandardCharsets.UTF_8), sessionId.toString());
    }

    /**
     * Validates the CSRF token against the session id in constant time.
     */
    public boolean validateCsrfToken(UUID sessionId, String csrfToken) {
        if (!StringUtils.hasText(csrfToken) || sessionId == null) {
            return false;
        }
        String expected = generateCsrfToken(sessionId);
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                csrfToken.getBytes(StandardCharsets.UTF_8)
        );
    }
}
