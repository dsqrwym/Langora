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
import org.dsqrwym.langora.auth.service.AuthCookieService;
import org.dsqrwym.langora.auth.service.AuthCsrfService;
import org.dsqrwym.langora.infrastructure.util.DeviceNameResolver;
import org.dsqrwym.langora.infrastructure.util.TextUtils;
import org.dsqrwym.langora.infrastructure.web.ApiResponse;
import org.dsqrwym.langora.infrastructure.web.ErrorCode;
import org.dsqrwym.langora.infrastructure.web.exception.ForbiddenException;
import org.dsqrwym.langora.infrastructure.web.exception.UnauthorizedException;
import org.springframework.http.HttpHeaders;
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
        final String userAgent = TextUtils.safeSubstring(TextUtils.defaultIfNull(req.getHeader(HttpHeaders.USER_AGENT), "unknown"), 500);

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
            HttpServletRequest req,
            HttpServletResponse res
    ) {
        String refreshToken = cookieService.extractRefreshToken(req);
        if (!StringUtils.hasText(refreshToken)) {
            throw new UnauthorizedException("Missing refresh token cookie");
        }

        UUID sessionId = jwtService.verifyAndExtractSessionId(refreshToken);

        if (!csrfService.validateCsrfToken(sessionId, csrfToken)) {
            throw new ForbiddenException(ErrorCode.FORBIDDEN, "Invalid or missing CSRF token");
        }

        RefreshTokenResponse response = authAdminService.refreshToken(refreshToken, sessionId, req.getRemoteAddr());

        cookieService.attachAuthCookies(res, response.getRefreshToken());
        // In webs do not need to return refresh token in JSON
        response.setRefreshToken(null);

        return ApiResponse.ok(response);
    }
}

