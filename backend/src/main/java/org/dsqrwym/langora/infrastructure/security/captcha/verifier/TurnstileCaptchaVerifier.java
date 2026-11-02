package org.dsqrwym.langora.infrastructure.security.captcha.verifier;

import lombok.extern.slf4j.Slf4j;
import org.dsqrwym.langora.infrastructure.security.captcha.CaptchaProperties;
import org.dsqrwym.langora.infrastructure.security.captcha.TurnstileResponse;
import org.dsqrwym.langora.infrastructure.web.exception.CaptchaException;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Objects;
import java.util.Set;

@Slf4j
public class TurnstileCaptchaVerifier implements CaptchaVerifier {
    private static final String PROVIDER = "turnstile";
    /**
     * Cloudflare 文档给的最大 token 长度
     */
    private static final int MAX_TOKEN_LENGTH = 2048;
    /**
     * 自己配错：无论 failOpen 如何都 fail-closed
     */
    private static final Set<String> CONFIGURATION_ERRORS =
            Set.of("missing-input-secret", "invalid-input-secret", "bad-request");
    /**
     * Cloudflare 出问题：跟随 failOpen
     */
    private static final Set<String> TRANSIENT_ERRORS = Set.of("internal-error");
    private final RestClient restClient;
    private final CaptchaProperties properties;

    public TurnstileCaptchaVerifier(RestClient restClient, CaptchaProperties properties) {
        this.restClient = restClient;
        this.properties = properties;
    }


    @Override
    public boolean enabled() {
        return properties.enabled();
    }

    @Override
    public String provider() {
        return PROVIDER;
    }

    @Override
    public String siteKey() {
        return properties.siteKey();
    }

    @Override
    public String action() {
        return properties.expectedAction();
    }

    @Override
    public void verify(String token, String remoteIp) {
        if (!StringUtils.hasText(token)) {
            throw CaptchaException.required();
        }
        if (token.length() > MAX_TOKEN_LENGTH) {
            throw CaptchaException.invalid();
        }

        TurnstileResponse response;

        try {
            response = restClient.post()
                    .uri(properties.verifyUrl())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form(token))
                    .retrieve()
                    .body(TurnstileResponse.class);
        } catch (RestClientException e) {
            handleTransportFailure("site verify call failed: " + e.getMessage());
            return;
        }

        if (response == null) {
            handleTransportFailure("site verify call returned an empty body");
            return;
        }

        if (response.success()) {
            validateBinding(response);
            return;
        }

        final List<String> errors = Objects.requireNonNullElse(response.errorCodes(), List.of());

        if (errors.stream().anyMatch(CONFIGURATION_ERRORS::contains)) {
            log.error("[Captcha] Turnstile rejected our configuration: {}", errors);
            throw CaptchaException.unavailable();
        }

        if (errors.stream().anyMatch(TRANSIENT_ERRORS::contains)) {
            handleTransportFailure("siteverify reported " + errors);
            return;
        }

        log.debug("[Captcha] Turnstile rejected the token: {}", errors);
        throw CaptchaException.invalid();
    }

    private MultiValueMap<String, String> form(String token) {
        final MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("secret", properties.secretKey());
        form.add("response", token);
        // remoteip is optional. Sending it adds no validation requirement, but a loopback/private
        // value can make Cloudflare answer bad-request, which would turn a valid login into a 503.
        // 不发送 remoteip：它是可选的，且私网/回环地址会触发 bad-request，导致登录 503。
        return form;
    }

    private void validateBinding(TurnstileResponse response) {
        final String expectedAction = properties.expectedAction();
        if (StringUtils.hasText(expectedAction) && !expectedAction.equals(response.action())) {
            log.warn("[Captcha] action mismatch expected='{}' actual='{}'", expectedAction, response.action());
            throw CaptchaException.invalid();
        }

        final List<String> allowed = properties.allowedHostnames();
        if (!allowed.isEmpty() && allowed.stream().noneMatch(h -> h.equalsIgnoreCase(response.hostname()))) {
            log.warn("[Captcha] hostname '{}' is not in {}", response.hostname(), allowed);
            throw CaptchaException.invalid();
        }
    }


    private void handleTransportFailure(String reason) {
        if (properties.failOpen()) {
            log.warn("[Captcha] {} - failing open", reason);
            return;
        }
        log.warn("[Captcha] {} - failing closed", reason);
        throw CaptchaException.unavailable();
    }
}
