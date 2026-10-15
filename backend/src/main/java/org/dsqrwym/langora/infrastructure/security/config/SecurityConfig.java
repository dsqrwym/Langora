package org.dsqrwym.langora.infrastructure.security.config;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.dsqrwym.langora.auth.jwt.UserJwtAuthenticationConverter;
import org.dsqrwym.langora.infrastructure.security.properties.CookieProperties;
import org.dsqrwym.langora.infrastructure.security.properties.CorsProperties;
import org.dsqrwym.langora.infrastructure.web.ApiResponse;
import org.dsqrwym.langora.infrastructure.web.ErrorCode;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.RequestCacheConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;

/**
 * Requests rejected by the filter chain never reach DispatcherServlet so they are handled by JsonErrorController
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity // Enable @PreAuthorize
@EnableConfigurationProperties({CookieProperties.class, CorsProperties.class})
@RequiredArgsConstructor
public class SecurityConfig {
    private final UserJwtAuthenticationConverter authenticationConverter;
    private final CorsProperties corsProperties;

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return (web) -> web.ignoring().requestMatchers(
                "/swagger-ui.html",
                "/swagger-ui/**",
                "/v3/api-docs/**",
                "/hello"
        );
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(corsProperties.allowedOrigins());
        config.setAllowedMethods(corsProperties.allowedMethods());
        config.setAllowedHeaders(corsProperties.allowedHeaders());
        config.setExposedHeaders(corsProperties.exposedHeaders());
        config.setAllowCredentials(corsProperties.allowCredentials());
        config.setMaxAge(corsProperties.maxAge());

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JsonMapper jsonMapper,
            @Qualifier(JwtConfig.ACCESS_TOKEN_DECODER) JwtDecoder accessTokenDecoder
    ) {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .requestCache(RequestCacheConfigurer::disable)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(auth ->
                        auth
                                .requestMatchers(
                                        "/auth/admin/login",
                                        "/auth/admin/refresh"
                                ).permitAll()
                                .anyRequest()
                                .authenticated()
                )

                .oauth2ResourceServer(oauth2 ->
                        oauth2
                                .jwt(jwt ->
                                        jwt
                                                .decoder(accessTokenDecoder)
                                                .jwtAuthenticationConverter(authenticationConverter)
                                )
                                .authenticationEntryPoint((_, res, _) ->
                                        write(res, HttpStatus.UNAUTHORIZED, ErrorCode.UNAUTHORIZED.name(), jsonMapper)
                                )
                                .accessDeniedHandler((_, res, _) ->
                                        write(res, HttpStatus.FORBIDDEN, ErrorCode.FORBIDDEN.name(), jsonMapper)
                                )
                )

                // Handle authentication and authorization errors
                .exceptionHandling(error ->
                        error.authenticationEntryPoint((_, res, _) ->
                                write(res, HttpStatus.UNAUTHORIZED, ErrorCode.UNAUTHORIZED.name(), jsonMapper)
                        ).accessDeniedHandler((_, res, _) ->
                                write(res, HttpStatus.FORBIDDEN, ErrorCode.FORBIDDEN.name(), jsonMapper)
                        )
                );
        return http.build();
    }

    private static void write(HttpServletResponse res, HttpStatus s,
                              String code, JsonMapper jsonMapper) throws IOException {
        res.setStatus(s.value());
        res.setContentType("application/json;charset=UTF-8");
        jsonMapper.writeValue(res.getWriter(),
                ApiResponse.error(s, code, s.getReasonPhrase(), null));
    }
}
