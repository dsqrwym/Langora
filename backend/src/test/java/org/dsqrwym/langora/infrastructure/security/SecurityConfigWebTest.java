package org.dsqrwym.langora.infrastructure.security;

import org.dsqrwym.langora.auth.UserTypes;
import org.dsqrwym.langora.auth.admin.service.AuthAdminService;
import org.dsqrwym.langora.auth.jwt.JwtTokenService;
import org.dsqrwym.langora.auth.jwt.UserJwtPayload;
import org.dsqrwym.langora.infrastructure.bootstrap.SuperAdminBootstrapRunner;
import org.junit.jupiter.api.Test;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.jooq.autoconfigure.JooqAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Exercises the real SecurityFilterChain and JWT decoder without a database. */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@EnableAutoConfiguration(exclude = {
        DataSourceAutoConfiguration.class,
        DataSourceTransactionManagerAutoConfiguration.class,
        FlywayAutoConfiguration.class,
        JooqAutoConfiguration.class
})
class SecurityConfigWebTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtTokenService jwtTokenService;

    @MockitoBean
    private DSLContext dslContext;
    @MockitoBean
    private PlatformTransactionManager transactionManager;
    @MockitoBean
    private SuperAdminBootstrapRunner superAdminBootstrapRunner;
    @MockitoBean
    private AuthAdminService authAdminService;

    @Test
    void anonymousProtectedEndpointReturnsUnifiedAccessTokenError() throws Exception {
        mockMvc.perform(post("/auth/admin/logout"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("AUTH_ACCESS_TOKEN_INVALID"));
    }

    @Test
    void validAccessTokenAuthenticatesLogoutRequest() throws Exception {
        UUID sessionId = UUID.randomUUID();
        String accessToken = jwtTokenService.generateAccessToken(
                new UserJwtPayload(7L, "Admin", "admin@langora.com", UserTypes.ADMIN,
                        List.of("ADMIN"), sessionId),
                Instant.now()
        );

        mockMvc.perform(post("/auth/admin/logout")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"));

        then(authAdminService).should().logout(
                org.mockito.ArgumentMatchers.eq(sessionId), org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.eq("Admin"), any(), any());
    }
}
