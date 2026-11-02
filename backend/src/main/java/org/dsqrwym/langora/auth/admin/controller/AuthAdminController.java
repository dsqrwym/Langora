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
import org.dsqrwym.langora.auth.jwt.UserJwtPayload;
import org.dsqrwym.langora.auth.service.AuthCookieService;
import org.dsqrwym.langora.auth.service.AuthCsrfService;
import org.dsqrwym.langora.infrastructure.util.DeviceNameResolver;
import org.dsqrwym.langora.infrastructure.util.TextUtils;
import org.dsqrwym.langora.infrastructure.web.ApiResponse;
import org.dsqrwym.langora.infrastructure.web.ErrorCode;
import org.dsqrwym.langora.infrastructure.web.exception.ForbiddenException;
import org.dsqrwym.langora.infrastructure.web.exception.UnauthorizedException;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;
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
                Objects.requireNonNullElse(req.getHeader(HttpHeaders.USER_AGENT), "unknown"),
                500
        );

        LoginResponseDto response = authAdminService.login(loginRequestDto, req.getRemoteAddr(), userAgent, DeviceNameResolver.resolveFromUserAgent(userAgent));

        cookieService.attachAuthCookies(res, response.getRefreshToken());
        // In webs do not need to return refresh token in JSON
        // Web 端不需要在 JSON 里返回 refresh token，只放在 HttpOnly Cookie 中
        response.setRefreshToken(null);

        response.setCsrfToken(csrfService.generateCsrfToken(response.getSessionId()));

        return ApiResponse.ok(response);
    }

    @PostMapping("/refresh")
    public ApiResponse<RefreshTokenResponse> refresh(
            @RequestHeader(value = AuthCsrfService.CSRF_HEADER_NAME, required = false) String csrfToken,
            HttpServletRequest req, HttpServletResponse res
    ) {

        final String refreshToken = cookieService.extractRefreshToken(req);
        if (!StringUtils.hasText(refreshToken)) {
            throw new UnauthorizedException(ErrorCode.AUTH_REFRESH_TOKEN_MISSING);
        }

        UUID sessionId;
        try {
            sessionId = jwtService.verifyAndExtractSessionId(refreshToken);
        } catch (UnauthorizedException e) {
            cookieService.clearAuthCookies(res);
            throw e;
        }

        if (!csrfService.validateCsrfToken(sessionId, csrfToken)) {
            throw new ForbiddenException(ErrorCode.AUTH_CSRF_INVALID);
        }

        final String userAgent = TextUtils.safeSubstring(
                Objects.requireNonNullElse(req.getHeader(HttpHeaders.USER_AGENT), "unknown"),
                500
        );

        try {
            RefreshTokenResponse response = authAdminService.refreshToken(
                    refreshToken, req.getRemoteAddr(), userAgent);

            cookieService.attachAuthCookies(res, response.getRefreshToken());
            response.setRefreshToken(null);
            response.setCsrfToken(csrfService.generateCsrfToken(sessionId));

            return ApiResponse.ok(response);
        } catch (UnauthorizedException | ForbiddenException e) {
            // Definitive auth failures only. 409 (in progress) and 5xx must keep the cookie.
            // 只清理确定的认证失败；409（并发刷新中）和 5xx 必须保留 cookie。
            cookieService.clearAuthCookies(res);
            throw e;
        }
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(
            @AuthenticationPrincipal UserJwtPayload payload,
            HttpServletRequest req, HttpServletResponse res) {

        final String userAgent = TextUtils.safeSubstring(
                Objects.requireNonNullElse(req.getHeader(HttpHeaders.USER_AGENT), "unknown"), 500);

        authAdminService.logout(
                payload.sessionId(), payload.userId(), payload.username(),
                req.getRemoteAddr(), userAgent);

        cookieService.clearAuthCookies(res);
        return ApiResponse.ok(null);
    }
}
