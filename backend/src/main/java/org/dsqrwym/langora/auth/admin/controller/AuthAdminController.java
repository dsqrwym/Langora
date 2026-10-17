package org.dsqrwym.langora.auth.admin.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.dsqrwym.langora.auth.admin.service.AuthAdminService;
import org.dsqrwym.langora.auth.dto.LoginRequestDto;
import org.dsqrwym.langora.auth.dto.LoginResponseDto;
import org.dsqrwym.langora.auth.dto.RefreshTokenResponse;
import org.dsqrwym.langora.auth.jwt.JwtTokenService;
import org.dsqrwym.langora.auth.jwt.UserJwtAuthenticationToken;
import org.dsqrwym.langora.auth.jwt.UserJwtPayload;
import org.dsqrwym.langora.auth.service.AuthCookieService;
import org.dsqrwym.langora.auth.service.AuthCsrfService;
import org.dsqrwym.langora.infrastructure.util.DeviceNameResolver;
import org.dsqrwym.langora.infrastructure.util.TextUtils;
import org.dsqrwym.langora.infrastructure.web.ApiResponse;
import org.dsqrwym.langora.infrastructure.web.ErrorCode;
import org.dsqrwym.langora.infrastructure.web.exception.ConflictException;
import org.dsqrwym.langora.infrastructure.web.exception.ForbiddenException;
import org.dsqrwym.langora.infrastructure.web.exception.UnauthorizedException;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/auth/admin")
@RequiredArgsConstructor
public class AuthAdminController {
    private final AuthAdminService authAdminService;
    private final AuthCookieService cookieService;
    private final AuthCsrfService csrfService;
    private final JwtTokenService jwtService;

    @PostMapping("/login")
    public ApiResponse<LoginResponseDto> login(@Valid @RequestBody LoginRequestDto loginRequestDto, HttpServletRequest req, HttpServletResponse res) {
        final String userAgent = TextUtils.safeSubstring(
                TextUtils.defaultIfNull(req.getHeader(HttpHeaders.USER_AGENT), "unknown"),
                500
        );

        LoginResponseDto response = authAdminService.login(loginRequestDto, req.getRemoteAddr(), userAgent, DeviceNameResolver.resolveFromUserAgent(userAgent));

        cookieService.attachAuthCookies(res, response.getRefreshToken());
        // In webs do not need to return refresh token in JSON
        response.setRefreshToken(null);

        response.setCsrfToken(csrfService.generateCsrfToken(response.getSessionId()));

        return ApiResponse.ok(response);
    }

    @PostMapping("/refresh")
    public ApiResponse<RefreshTokenResponse> refresh(
            @RequestHeader(value = AuthCsrfService.CSRF_HEADER_NAME, required = false) String csrfToken,
            HttpServletRequest req, HttpServletResponse res
    ) {

        String refreshToken = cookieService.extractRefreshToken(req);
        if (!StringUtils.hasText(refreshToken)) {
            throw new UnauthorizedException("Missing refresh token cookie");
        }

        UUID sessionId = jwtService.verifyAndExtractSessionId(refreshToken);

        if (!csrfService.validateCsrfToken(sessionId, csrfToken)) {
            throw new ForbiddenException(ErrorCode.FORBIDDEN, "Invalid or missing CSRF token");
        }

        final String userAgent = TextUtils.safeSubstring(
                TextUtils.defaultIfNull(req.getHeader(HttpHeaders.USER_AGENT), "unknown"),
                500
        );

        try {
            RefreshTokenResponse response = authAdminService.refreshToken(
                    refreshToken, sessionId, req.getRemoteAddr(), userAgent);

            cookieService.attachAuthCookies(res, response.getRefreshToken());
            response.setRefreshToken(null);
            response.setCsrfToken(csrfService.generateCsrfToken(sessionId));

            return ApiResponse.ok(response);
        } catch (ConflictException e) {
            // AUTH_REFRESH_IN_PROGRESS: a concurrent request already rotated this token.
            // Keep the cookie — the client must wait for the winning request's Set-Cookie.
            throw e;
        } catch (Exception e) {
            // Any other failure (reuse detected, revoked, expired, account disabled):
            // the cookie is now unusable; clear it to prevent the browser from looping forever.
            cookieService.clearAuthCookies(res);
            throw e;
        }
    }

    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Void> logout(
            @AuthenticationPrincipal UserJwtAuthenticationToken auth,
            HttpServletRequest req, HttpServletResponse res) {

        UserJwtPayload payload = auth.getPrincipal();
        final String userAgent = TextUtils.safeSubstring(
                TextUtils.defaultIfNull(req.getHeader(HttpHeaders.USER_AGENT), "unknown"), 500);

        authAdminService.logout(
                payload.sessionId(), payload.userId(), payload.username(),
                req.getRemoteAddr(), userAgent);

        cookieService.clearAuthCookies(res);
        return ApiResponse.ok(null);
    }
}
