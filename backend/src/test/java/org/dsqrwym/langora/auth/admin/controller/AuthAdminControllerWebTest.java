package org.dsqrwym.langora.auth.admin.controller;

import jakarta.servlet.http.Cookie;
import org.dsqrwym.langora.auth.UserTypes;
import org.dsqrwym.langora.auth.admin.service.AuthAdminService;
import org.dsqrwym.langora.auth.dto.LoginRequestDto;
import org.dsqrwym.langora.auth.dto.LoginResponseDto;
import org.dsqrwym.langora.auth.dto.RefreshTokenResponse;
import org.dsqrwym.langora.auth.jwt.JwtTokenService;
import org.dsqrwym.langora.auth.service.AuthCookieService;
import org.dsqrwym.langora.auth.service.AuthCsrfService;
import org.dsqrwym.langora.infrastructure.web.ErrorCode;
import org.dsqrwym.langora.infrastructure.web.exception.ConflictException;
import org.dsqrwym.langora.infrastructure.web.exception.UnauthorizedException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthAdminController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class AuthAdminControllerWebTest {

    private static final UUID SESSION_ID = UUID.fromString("00000000-0000-0000-0000-000000000007");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthAdminService authAdminService;
    @MockitoBean
    private AuthCookieService cookieService;
    @MockitoBean
    private AuthCsrfService csrfService;
    @MockitoBean
    private JwtTokenService jwtTokenService;

    @Test
    void loginNormalizesEmailAndNeverReturnsRefreshTokenValueInJson() throws Exception {
        LoginResponseDto serviceResponse = new LoginResponseDto(
                "access", "refresh", 7L, "Admin", "admin@langora.com", UserTypes.ADMIN,
                List.of("ADMIN"), SESSION_ID);
        given(authAdminService.login(any(LoginRequestDto.class), anyString(), anyString(), anyString()))
                .willReturn(serviceResponse);
        given(csrfService.generateCsrfToken(SESSION_ID)).willReturn("csrf");

        mockMvc.perform(post("/auth/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0) Chrome/130.0")
                        .content("""
                                {"email":"  ADMIN@LANGORA.COM ","password":"password","deviceUuid":"device-1"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.accessToken").value("access"))
                .andExpect(jsonPath("$.data.csrfToken").value("csrf"))
                .andExpect(jsonPath("$.data.sessionId").value(SESSION_ID.toString()))
                .andExpect(jsonPath("$.data.refreshToken").value(nullValue()));

        ArgumentCaptor<LoginRequestDto> request = ArgumentCaptor.forClass(LoginRequestDto.class);
        then(authAdminService).should().login(request.capture(), anyString(), anyString(), anyString());
        assertEquals("admin@langora.com", request.getValue().email());
        then(cookieService).should().attachAuthCookies(any(), eq("refresh"));
    }

    @Test
    void loginReturnsStructuredValidationErrorForBothIdentifiers() throws Exception {
        mockMvc.perform(post("/auth/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"admin@langora.com","username":"Admin","password":"password","deviceUuid":"device-1"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.data[0].code").value("VALIDATION_INVALID_COMBINATION"));

        then(authAdminService).shouldHaveNoInteractions();
    }

    @Test
    void loginMapsInvalidCredentialsToErrorContract() throws Exception {
        given(authAdminService.login(any(LoginRequestDto.class), anyString(), anyString(), anyString()))
                .willThrow(new UnauthorizedException(ErrorCode.AUTH_INVALID_CREDENTIALS));

        mockMvc.perform(post("/auth/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"Admin","password":"wrong","deviceUuid":"device-1"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.data").value(nullValue()));
    }

    @Test
    void refreshWithoutCookieReturnsDedicatedErrorCode() throws Exception {
        mockMvc.perform(post("/auth/admin/refresh"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_REFRESH_TOKEN_MISSING"));

        then(authAdminService).shouldHaveNoInteractions();
    }

    @Test
    void refreshWithInvalidCsrfStopsBeforeServiceCall() throws Exception {
        given(cookieService.extractRefreshToken(any())).willReturn("refresh");
        given(jwtTokenService.verifyAndExtractSessionId("refresh")).willReturn(SESSION_ID);
        given(csrfService.validateCsrfToken(SESSION_ID, "invalid")).willReturn(false);

        mockMvc.perform(post("/auth/admin/refresh")
                        .cookie(new Cookie(AuthCookieService.REFRESH_COOKIE_NAME, "refresh"))
                        .header(AuthCsrfService.CSRF_HEADER_NAME, "invalid"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH_CSRF_INVALID"));

        then(authAdminService).shouldHaveNoInteractions();
        then(cookieService).should().extractRefreshToken(any());
        then(cookieService).shouldHaveNoMoreInteractions();
    }

    @Test
    void refreshSuccessWritesCookieAndHidesRefreshTokenValue() throws Exception {
        RefreshTokenResponse response = new RefreshTokenResponse("new-access", "new-refresh");
        prepareValidRefresh("refresh");
        given(authAdminService.refreshToken(eq("refresh"), eq(SESSION_ID), anyString(), anyString())).willReturn(response);
        given(csrfService.generateCsrfToken(SESSION_ID)).willReturn("new-csrf");

        mockMvc.perform(post("/auth/admin/refresh")
                        .cookie(new Cookie(AuthCookieService.REFRESH_COOKIE_NAME, "refresh"))
                        .header(AuthCsrfService.CSRF_HEADER_NAME, "csrf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.accessToken").value("new-access"))
                .andExpect(jsonPath("$.data.csrfToken").value("new-csrf"))
                .andExpect(jsonPath("$.data.refreshToken").value(nullValue()));

        then(cookieService).should().attachAuthCookies(any(), eq("new-refresh"));
    }

    @Test
    void refreshConflictKeepsExistingCookieForWinningRequest() throws Exception {
        prepareValidRefresh("refresh");
        given(authAdminService.refreshToken(eq("refresh"), eq(SESSION_ID), anyString(), anyString()))
                .willThrow(new ConflictException(ErrorCode.AUTH_REFRESH_IN_PROGRESS));

        mockMvc.perform(post("/auth/admin/refresh")
                        .cookie(new Cookie(AuthCookieService.REFRESH_COOKIE_NAME, "refresh"))
                        .header(AuthCsrfService.CSRF_HEADER_NAME, "csrf"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("AUTH_REFRESH_IN_PROGRESS"));

        then(cookieService).should(never()).clearAuthCookies(any());
    }

    @Test
    void refreshReuseClearsUnusableCookie() throws Exception {
        prepareValidRefresh("refresh");
        given(authAdminService.refreshToken(eq("refresh"), eq(SESSION_ID), anyString(), anyString()))
                .willThrow(new UnauthorizedException(ErrorCode.AUTH_REFRESH_REUSED));

        mockMvc.perform(post("/auth/admin/refresh")
                        .cookie(new Cookie(AuthCookieService.REFRESH_COOKIE_NAME, "refresh"))
                        .header(AuthCsrfService.CSRF_HEADER_NAME, "csrf"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_REFRESH_REUSED"));

        then(cookieService).should().clearAuthCookies(any());
    }

    private void prepareValidRefresh(String token) {
        given(cookieService.extractRefreshToken(any())).willReturn(token);
        given(jwtTokenService.verifyAndExtractSessionId(token)).willReturn(SESSION_ID);
        given(csrfService.validateCsrfToken(SESSION_ID, "csrf")).willReturn(true);
    }
}
