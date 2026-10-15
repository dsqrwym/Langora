package org.dsqrwym.langora.infrastructure.security.config;

import org.dsqrwym.langora.auth.jwt.MandatoryClaimsValidator;
import org.dsqrwym.langora.infrastructure.security.properties.JwtProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

/**
 * Register official Nimbus encoder and decoder
 */
@Configuration
public class JwtConfig {
    public static final String ACCESS_TOKEN_DECODER  = "accessTokenDecoder";
    public static final String REFRESH_TOKEN_DECODER = "refreshTokenDecoder";

    @Bean
    public SecretKey jwtSecretKey(JwtProperties jwtProperties) {
        // "HmacSHA256"
        return new SecretKeySpec(
                jwtProperties.secretKey().getBytes(StandardCharsets.UTF_8),
                "HmacSHA256"
        );
    }

    @Bean(ACCESS_TOKEN_DECODER)
    public JwtDecoder accessTokenDecoder(SecretKey secretKey) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(secretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();

        OAuth2TokenValidator<Jwt> defaultValidator = JwtValidators.createDefault();
        OAuth2TokenValidator<Jwt> mandatoryValidator = new MandatoryClaimsValidator();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(defaultValidator, mandatoryValidator));

        return decoder;
    }

    @Bean(REFRESH_TOKEN_DECODER)
    public JwtDecoder refreshTokenDecoder(SecretKey secretKey) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(secretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();

        decoder.setJwtValidator(JwtValidators.createDefault());
        return decoder;
    }

    @Bean
    public JwtEncoder jwtEncoder(SecretKey secretKey) {
        return NimbusJwtEncoder
                .withSecretKey(secretKey)
                .algorithm(MacAlgorithm.HS256)
                .build();
    }
}
