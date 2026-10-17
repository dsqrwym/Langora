package org.dsqrwym.langora.auth.service;

import jakarta.servlet.http.Cookie;
import org.dsqrwym.langora.infrastructure.security.properties.CookieProperties;
import org.dsqrwym.langora.infrastructure.security.properties.JwtProperties;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthCookieServiceTest {

    private final AuthCookieService service = new AuthCookieService(
            new CookieProperties(null, true, "Strict", "/langora/auth/admin/refresh"),
            new JwtProperties("test-only-jwt-secret-key-must-be-at-least-32-chars", "langora-test",
                    Duration.ofMinutes(15), Duration.ofDays(1))
    );

    @Test
    void attachesRestrictedHttpOnlyRefreshCookie() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        service.attachAuthCookies(response, "refresh-value");

        String cookie = response.getHeader("Set-Cookie");
        assertTrue(cookie.contains("REFRESH-TOKEN=refresh-value"));
        assertTrue(cookie.contains("Path=/langora/auth/admin/refresh"));
        assertTrue(cookie.contains("HttpOnly"));
        assertTrue(cookie.contains("Secure"));
        assertTrue(cookie.contains("SameSite=Strict"));
    }

    @Test
    void clearsCookieWithSameScope() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        service.clearAuthCookies(response);

        String cookie = response.getHeader("Set-Cookie");
        assertTrue(cookie.contains("REFRESH-TOKEN="));
        assertTrue(cookie.contains("Path=/langora/auth/admin/refresh"));
        assertTrue(cookie.contains("Max-Age=0"));
    }

    @Test
    void extractsOnlyRefreshCookie() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("other", "ignored"), new Cookie(AuthCookieService.REFRESH_COOKIE_NAME, "refresh"));

        assertEquals("refresh", service.extractRefreshToken(request));
        assertNull(service.extractRefreshToken(new MockHttpServletRequest()));
    }
}
