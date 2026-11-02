package org.dsqrwym.langora.infrastructure.config;

import org.dsqrwym.langora.auth.admin.config.AuthAdminSessionProperties;
import org.dsqrwym.langora.infrastructure.bootstrap.SuperAdminBootstrapProperties;
import org.dsqrwym.langora.infrastructure.security.captcha.CaptchaProperties;
import org.dsqrwym.langora.infrastructure.security.login.LoginAttemptProperties;
import org.dsqrwym.langora.infrastructure.security.properties.CookieProperties;
import org.dsqrwym.langora.infrastructure.security.properties.CorsProperties;
import org.dsqrwym.langora.infrastructure.security.properties.CsrfProperties;
import org.dsqrwym.langora.infrastructure.security.properties.JwtProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Binding tests for the configuration records. They deliberately use {@link ApplicationContextRunner}
 * instead of a full {@code @SpringBootTest} so the defaults and validation rules can be exercised
 * without a database or Mockito.
 */
class ConfigurationPropertiesValidationTest {

    private static final String JWT_SECRET = "test-only-jwt-secret-key-must-be-at-least-32-chars";
    private static final String CSRF_SECRET = "test-only-csrf-secret-key-must-be-at-least-32-chars";

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(PropertiesConfiguration.class)
            .withPropertyValues(
                    "langora.security.jwt.secret-key=" + JWT_SECRET,
                    "langora.security.csrf.secret-key=" + CSRF_SECRET,
                    "langora.security.cookie.refresh-token-path=/langora/auth/admin/refresh",
                    "langora.security.cors.allowed-origins[0]=http://127.0.0.1:5173"
            );

    @Test
    void appliesDefaultsWhenOptionalPropertiesAreAbsent() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();

            AuthAdminSessionProperties session = context.getBean(AuthAdminSessionProperties.class);
            assertThat(session.superAdminMaxSession()).isEqualTo(3);
            assertThat(session.adminMaxSession()).isEqualTo(5);
            assertThat(session.refreshGraceSeconds()).isEqualTo(5);
            assertThat(session.cleanup()).isNotNull();
            assertThat(session.cleanup().retention()).isEqualTo(Duration.ofDays(365));
            assertThat(session.cleanup().batchSize()).isEqualTo(1000);
            assertThat(session.cleanup().zone()).isEqualTo("Europe/Madrid");

            JwtProperties jwt = context.getBean(JwtProperties.class);
            assertThat(jwt.issuer()).isEqualTo("langora-auth");
            assertThat(jwt.accessTokenExpiration()).isEqualTo(Duration.ofMinutes(15));
            assertThat(jwt.refreshTokenExpiration()).isEqualTo(Duration.ofDays(1));

            CookieProperties cookie = context.getBean(CookieProperties.class);
            assertThat(cookie.secure()).isTrue();
            assertThat(cookie.sameSite()).isEqualTo("Lax");

            CorsProperties cors = context.getBean(CorsProperties.class);
            assertThat(cors.allowedMethods()).contains("GET", "OPTIONS");
            assertThat(cors.allowedHeaders()).contains("Authorization");
            assertThat(cors.maxAge()).isEqualTo(3600);

            SuperAdminBootstrapProperties bootstrap = context.getBean(SuperAdminBootstrapProperties.class);
            assertThat(bootstrap.enabled()).isFalse();
            assertThat(bootstrap.credentialsIgnored()).isFalse();

            CaptchaProperties captcha = context.getBean(CaptchaProperties.class);
            assertThat(captcha.enabled()).isFalse();
            assertThat(captcha.siteKey()).isEmpty();
            assertThat(captcha.secretKey()).isEmpty();
            assertThat(captcha.allowedHostnames()).isEmpty();
            assertThat(captcha.failOpen()).isTrue();

            LoginAttemptProperties loginAttempt = context.getBean(LoginAttemptProperties.class);
            assertThat(loginAttempt.store()).isEqualTo(LoginAttemptProperties.Store.MEMORY);
            assertThat(loginAttempt.ipBlockThreshold()).isEqualTo(20);
            assertThat(loginAttempt.captchaThreshold()).isEqualTo(3);
            assertThat(loginAttempt.accountBlockThreshold()).isEqualTo(5);
            assertThat(loginAttempt.window()).isEqualTo(Duration.ofMinutes(15));
            assertThat(loginAttempt.maxLocalEntries()).isEqualTo(50_000);
        });
    }

    @Test
    void bootstrapCanStayDisabledWithoutCredentials() {
        runner.withPropertyValues("langora.bootstrap.super-admin.enabled=false")
                .run(context -> assertThat(context).hasNotFailed());
    }

    @Test
    void bootstrapEnabledRequiresAllCredentials() {
        runner.withPropertyValues("langora.bootstrap.super-admin.enabled=true")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void bootstrapEnabledWithCredentialsBinds() {
        runner.withPropertyValues(
                        "langora.bootstrap.super-admin.enabled=true",
                        "langora.bootstrap.super-admin.email=superadmin@langora.com",
                        "langora.bootstrap.super-admin.username=SuperAdmin",
                        "langora.bootstrap.super-admin.password=SuperAdmin123")
                .run(context -> assertThat(context).hasNotFailed());
    }

    @Test
    void productionStyleEmptyCredentialsAreAcceptedWhileDisabled() {
        runner.withPropertyValues(
                        "langora.bootstrap.super-admin.enabled=false",
                        "langora.bootstrap.super-admin.email=",
                        "langora.bootstrap.super-admin.username=",
                        "langora.bootstrap.super-admin.password=")
                .run(context -> assertThat(context).hasNotFailed());
    }

    @Test
    void productionStyleEmptyCredentialsFailWhenEnabled() {
        runner.withPropertyValues(
                        "langora.bootstrap.super-admin.enabled=true",
                        "langora.bootstrap.super-admin.email=",
                        "langora.bootstrap.super-admin.username=",
                        "langora.bootstrap.super-admin.password=")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void disabledBootstrapIgnoresMalformedCredentials() {
        runner.withPropertyValues(
                        "langora.bootstrap.super-admin.enabled=false",
                        "langora.bootstrap.super-admin.email=not-an-email",
                        "langora.bootstrap.super-admin.username=x",
                        "langora.bootstrap.super-admin.password=weak")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    SuperAdminBootstrapProperties bootstrap = context.getBean(SuperAdminBootstrapProperties.class);
                    assertThat(bootstrap.email()).isNull();
                    assertThat(bootstrap.username()).isNull();
                    assertThat(bootstrap.password()).isNull();
                    assertThat(bootstrap.credentialsIgnored()).isTrue();
                });
    }

    @Test
    void enabledBootstrapStillRejectsMalformedCredentials() {
        runner.withPropertyValues(
                        "langora.bootstrap.super-admin.enabled=true",
                        "langora.bootstrap.super-admin.email=superadmin@langora.com",
                        "langora.bootstrap.super-admin.username=SuperAdmin",
                        "langora.bootstrap.super-admin.password=weak")
                .run(context -> assertThat(context).hasFailed());

        runner.withPropertyValues(
                        "langora.bootstrap.super-admin.enabled=true",
                        "langora.bootstrap.super-admin.email=not-an-email",
                        "langora.bootstrap.super-admin.username=SuperAdmin",
                        "langora.bootstrap.super-admin.password=SuperAdmin123")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void emptySessionLimitFailsAtStartup() {
        runner.withPropertyValues("langora.auth.admin.session.super-admin-max-session=")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void emptyRetentionFallsBackToDefault() {
        runner.withPropertyValues("langora.auth.admin.session.cleanup.retention=")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(AuthAdminSessionProperties.class).cleanup().retention())
                            .isEqualTo(Duration.ofDays(365));
                });
    }

    @Test
    void refreshTokenMustOutliveAccessToken() {
        runner.withPropertyValues(
                        "langora.security.jwt.access-token-expiration=1d",
                        "langora.security.jwt.refresh-token-expiration=15m")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void sameSiteNoneRequiresSecureCookie() {
        runner.withPropertyValues(
                        "langora.security.cookie.same-site=None",
                        "langora.security.cookie.secure=false")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void credentialedCorsRejectsWildcardOrigin() {
        runner.withPropertyValues(
                        "langora.security.cors.allowed-origins[1]=*",
                        "langora.security.cors.allow-credentials=true")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void corsRequiresAtLeastOneOrigin() {
        new ApplicationContextRunner()
                .withUserConfiguration(CorsOnlyConfiguration.class)
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void captchaEnabledRequiresSiteAndSecretKeys() {
        new ApplicationContextRunner()
                .withUserConfiguration(CaptchaOnlyConfiguration.class)
                .withPropertyValues("langora.security.captcha.enabled=true")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void captchaEnabledWithKeysStarts() {
        new ApplicationContextRunner()
                .withUserConfiguration(CaptchaOnlyConfiguration.class)
                .withPropertyValues(
                        "langora.security.captcha.enabled=true",
                        "langora.security.captcha.site-key=site",
                        "langora.security.captcha.secret-key=secret")
                .run(context -> assertThat(context).hasNotFailed());
    }

    @Test
    void captchaThresholdMustBeBelowIpBlockThreshold() {
        new ApplicationContextRunner()
                .withUserConfiguration(LoginAttemptOnlyConfiguration.class)
                .withPropertyValues("langora.security.login-attempt.captcha-threshold=20")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void loginAttemptThresholdsMustBePositive() {
        new ApplicationContextRunner()
                .withUserConfiguration(LoginAttemptOnlyConfiguration.class)
                .withPropertyValues("langora.security.login-attempt.account-block-threshold=0")
                .run(context -> assertThat(context).hasFailed());
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties({
            AuthAdminSessionProperties.class,
            JwtProperties.class,
            CsrfProperties.class,
            CookieProperties.class,
            CorsProperties.class,
            SuperAdminBootstrapProperties.class,
            CaptchaProperties.class,
            LoginAttemptProperties.class
    })
    static class PropertiesConfiguration {
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(CaptchaProperties.class)
    static class CaptchaOnlyConfiguration {
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(LoginAttemptProperties.class)
    static class LoginAttemptOnlyConfiguration {
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(CorsProperties.class)
    static class CorsOnlyConfiguration {
    }
}
