package org.dsqrwym.langora.infrastructure.security.captcha;

import lombok.extern.slf4j.Slf4j;
import org.dsqrwym.langora.infrastructure.security.captcha.verifier.CaptchaVerifier;
import org.dsqrwym.langora.infrastructure.security.captcha.verifier.NoopCaptchaVerifier;
import org.dsqrwym.langora.infrastructure.security.captcha.verifier.TurnstileCaptchaVerifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;

@Slf4j
@Configuration(proxyBeanMethods = false)
public class CaptchaConfig {

    @Bean
    @ConditionalOnProperty(prefix = "langora.security.captcha", name = "enabled", havingValue = "true")
    public CaptchaVerifier turnstileCaptchaVerifier(CaptchaProperties properties) {
        log.info("[Captcha] Turnstile enabled (siteKey={}, action={}, hostnames={}, failOpen={})",
                properties.siteKey(),
                properties.expectedAction(),
                properties.allowedHostnames().isEmpty() ? "<unchecked>" : properties.allowedHostnames(),
                properties.failOpen());
        final HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.connectTimeout())
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();

        final JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(properties.readTimeout());

        return new TurnstileCaptchaVerifier(
                RestClient
                        .builder()
                        .requestFactory(requestFactory)
                        .build()
                , properties
        );
    }

    @Bean
    @ConditionalOnProperty(prefix = "langora.security.captcha", name = "enabled", havingValue = "false", matchIfMissing = true)
    public CaptchaVerifier noopCaptchaVerifier() {
        log.info("[Captcha] disabled: login does not require a token");
        return NoopCaptchaVerifier.INSTANCE;
    }
}
