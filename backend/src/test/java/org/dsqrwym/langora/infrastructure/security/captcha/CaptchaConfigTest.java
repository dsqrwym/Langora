package org.dsqrwym.langora.infrastructure.security.captcha;

import org.dsqrwym.langora.infrastructure.security.captcha.verifier.CaptchaVerifier;
import org.dsqrwym.langora.infrastructure.security.captcha.verifier.NoopCaptchaVerifier;
import org.dsqrwym.langora.infrastructure.security.captcha.verifier.TurnstileCaptchaVerifier;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the conditional CAPTCHA bean: Noop by default, Turnstile when enabled with keys.
 */
class CaptchaConfigTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(CaptchaConfig.class, PropertiesConfiguration.class);

    @Test
    void disabledCaptchaProvidesTheNoopVerifier() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBean(CaptchaVerifier.class))
                    .isInstanceOf(NoopCaptchaVerifier.class);
            assertThat(context.getBean(CaptchaVerifier.class).enabled()).isFalse();
        });
    }

    @Test
    void enabledCaptchaWithKeysProvidesTurnstile() {
        runner.withPropertyValues(
                        "langora.security.captcha.enabled=true",
                        "langora.security.captcha.site-key=site",
                        "langora.security.captcha.secret-key=secret",
                        "langora.security.captcha.allowed-hostnames[0]=langora.dsqrwym.es")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(CaptchaVerifier.class))
                            .isInstanceOf(TurnstileCaptchaVerifier.class);
                    assertThat(context.getBean(CaptchaVerifier.class).siteKey()).isEqualTo("site");
                    assertThat(context.getBean(CaptchaVerifier.class).action()).isEqualTo("login");
                });
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(CaptchaProperties.class)
    static class PropertiesConfiguration {
    }
}
