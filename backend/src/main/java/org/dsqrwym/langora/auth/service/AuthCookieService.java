package org.dsqrwym.langora.auth.service;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.dsqrwym.langora.infrastructure.security.properties.CookieProperties;
import org.dsqrwym.langora.infrastructure.security.properties.JwtProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AuthCookieService {
    public static final String REFRESH_COOKIE_NAME = "REFRESH-TOKEN";

    private final CookieProperties cookieProperties;
    private final JwtProperties jwtProperties;
    private final String cookieDomain;

    public AuthCookieService(CookieProperties cookieProperties, JwtProperties jwtProperties) {
        this.cookieProperties = cookieProperties;
        this.jwtProperties = jwtProperties;
        this.cookieDomain = StringUtils.hasText(cookieProperties.domain())
                ? cookieProperties.domain()
                : null;
    }

    /**
     * Write cookies and CSRF Cookie into response for Web
     */
    public void attachAuthCookies(HttpServletResponse response, String refreshToken) {
        ResponseCookie.ResponseCookieBuilder refreshBuilder = ResponseCookie
                .from(REFRESH_COOKIE_NAME, refreshToken)
                .httpOnly(true)
                .secure(cookieProperties.secure())
                .path(cookieProperties.refreshTokenPath())
                .maxAge(jwtProperties.refreshTokenExpiration())
                .sameSite(cookieProperties.sameSite());

        if (cookieDomain != null) {
            refreshBuilder.domain(cookieDomain);
        }
        response.addHeader(HttpHeaders.SET_COOKIE, refreshBuilder.build().toString());
    }

    /**
     * Remove all Cookies
     */
    public void clearAuthCookies(HttpServletResponse response) {
        final boolean hasDomain = StringUtils.hasText(cookieProperties.domain());

        ResponseCookie.ResponseCookieBuilder refreshBuilder = ResponseCookie
                .from(REFRESH_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(cookieProperties.secure())
                .path(cookieProperties.refreshTokenPath())
                .maxAge(0)
                .sameSite(cookieProperties.sameSite());

        if (cookieDomain != null) {
            refreshBuilder.domain(cookieDomain);
        }
        response.addHeader(HttpHeaders.SET_COOKIE, refreshBuilder.build().toString());
    }

    public String extractRefreshToken(HttpServletRequest request) {
        final jakarta.servlet.http.Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (REFRESH_COOKIE_NAME.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
