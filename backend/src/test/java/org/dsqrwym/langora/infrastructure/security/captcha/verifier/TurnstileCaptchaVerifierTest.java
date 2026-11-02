package org.dsqrwym.langora.infrastructure.security.captcha.verifier;

import org.dsqrwym.langora.infrastructure.security.captcha.CaptchaProperties;
import org.dsqrwym.langora.infrastructure.web.exception.CaptchaException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Siteverify contract tests. They never call Cloudflare: MockRestServiceServer captures the request.
 */
class TurnstileCaptchaVerifierTest {

    private static final String VERIFY_URL = "https://challenges.cloudflare.com/turnstile/v0/siteverify";
    private static final String HOSTNAME = "langora.dsqrwym.es";

    @Test
    void acceptsValidTokenAndSendsTheExpectedFormFields() {
        TestContext context = context(true);

        context.server().expect(requestTo(VERIFY_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string(containsString("secret=secret")))
                .andExpect(content().string(containsString("response=token")))
                .andExpect(content().string(not(containsString("remoteip"))))
                .andRespond(withSuccess("""
                        {"success":true,"hostname":"langora.dsqrwym.es","action":"login","error-codes":[]}
                        """, MediaType.APPLICATION_JSON));

        assertDoesNotThrow(() -> context.verifier().verify("token", "203.0.113.7"));
        context.server().verify();
    }

    @Test
    void neverSendsTheVisitorIpAsRemoteIp() {
        TestContext context = context(true);

        context.server().expect(requestTo(VERIFY_URL))
                .andExpect(content().string(not(containsString("remoteip"))))
                .andRespond(withSuccess("""
                        {"success":true,"hostname":"langora.dsqrwym.es","action":"login","error-codes":[]}
                        """, MediaType.APPLICATION_JSON));

        assertDoesNotThrow(() -> context.verifier().verify("token", "203.0.113.7"));
        context.server().verify();
    }

    @Test
    void rejectsBlankTokenWithoutCallingSiteverify() {
        TestContext context = context(true);

        CaptchaException exception = assertThrows(CaptchaException.class,
                () -> context.verifier().verify("  ", "203.0.113.7"));

        assertEquals("AUTH_CAPTCHA_REQUIRED", exception.code.name());
        context.server().verify();
    }

    @Test
    void rejectsOverlongTokenWithoutCallingSiteverify() {
        TestContext context = context(true);

        CaptchaException exception = assertThrows(CaptchaException.class,
                () -> context.verifier().verify("a".repeat(2049), "203.0.113.7"));

        assertEquals("AUTH_CAPTCHA_INVALID", exception.code.name());
        context.server().verify();
    }

    @Test
    void rejectsInvalidInputResponse() {
        TestContext context = context(true);
        context.server().expect(requestTo(VERIFY_URL))
                .andRespond(withSuccess("""
                        {"success":false,"error-codes":["invalid-input-response"]}
                        """, MediaType.APPLICATION_JSON));

        CaptchaException exception = assertThrows(CaptchaException.class,
                () -> context.verifier().verify("token", "203.0.113.7"));

        assertEquals("AUTH_CAPTCHA_INVALID", exception.code.name());
    }

    @Test
    void rejectsDuplicateOrExpiredToken() {
        TestContext context = context(true);
        context.server().expect(requestTo(VERIFY_URL))
                .andRespond(withSuccess("""
                        {"success":false,"error-codes":["timeout-or-duplicate"]}
                        """, MediaType.APPLICATION_JSON));

        CaptchaException exception = assertThrows(CaptchaException.class,
                () -> context.verifier().verify("token", "203.0.113.7"));

        assertEquals("AUTH_CAPTCHA_INVALID", exception.code.name());
    }

    @Test
    void rejectsActionMismatch() {
        TestContext context = context(true);
        context.server().expect(requestTo(VERIFY_URL))
                .andRespond(withSuccess("""
                        {"success":true,"hostname":"langora.dsqrwym.es","action":"other","error-codes":[]}
                        """, MediaType.APPLICATION_JSON));

        CaptchaException exception = assertThrows(CaptchaException.class,
                () -> context.verifier().verify("token", "203.0.113.7"));

        assertEquals("AUTH_CAPTCHA_INVALID", exception.code.name());
    }

    @Test
    void rejectsHostnameOutsideTheAllowList() {
        TestContext context = context(true);
        context.server().expect(requestTo(VERIFY_URL))
                .andRespond(withSuccess("""
                        {"success":true,"hostname":"evil.example.com","action":"login","error-codes":[]}
                        """, MediaType.APPLICATION_JSON));

        CaptchaException exception = assertThrows(CaptchaException.class,
                () -> context.verifier().verify("token", "203.0.113.7"));

        assertEquals("AUTH_CAPTCHA_INVALID", exception.code.name());
    }

    @Test
    void acceptsAnyHostnameWhenNoAllowListIsConfigured() {
        TestContext context = context(true, List.of());
        context.server().expect(requestTo(VERIFY_URL))
                .andRespond(withSuccess("""
                        {"success":true,"hostname":"anything.example.com","action":"login","error-codes":[]}
                        """, MediaType.APPLICATION_JSON));

        assertDoesNotThrow(() -> context.verifier().verify("token", "203.0.113.7"));
    }

    @Test
    void secretMisconfigurationFailsClosedEvenWhenFailOpenIsTrue() {
        TestContext context = context(true);
        context.server().expect(requestTo(VERIFY_URL))
                .andRespond(withSuccess("""
                        {"success":false,"error-codes":["invalid-input-secret"]}
                        """, MediaType.APPLICATION_JSON));

        CaptchaException exception = assertThrows(CaptchaException.class,
                () -> context.verifier().verify("token", "203.0.113.7"));

        assertEquals("AUTH_CAPTCHA_UNAVAILABLE", exception.code.name());
    }

    @Test
    void providerOutageFailsOpenWhenConfigured() {
        TestContext context = context(true);
        context.server().expect(requestTo(VERIFY_URL))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertDoesNotThrow(() -> context.verifier().verify("token", "203.0.113.7"));
    }

    @Test
    void providerOutageFailsClosedWhenConfigured() {
        TestContext context = context(false);
        context.server().expect(requestTo(VERIFY_URL))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        CaptchaException exception = assertThrows(CaptchaException.class,
                () -> context.verifier().verify("token", "203.0.113.7"));

        assertEquals("AUTH_CAPTCHA_UNAVAILABLE", exception.code.name());
    }

    @Test
    void internalErrorFailsOpenWhenConfigured() {
        TestContext context = context(true);
        context.server().expect(requestTo(VERIFY_URL))
                .andRespond(withSuccess("""
                        {"success":false,"error-codes":["internal-error"]}
                        """, MediaType.APPLICATION_JSON));

        assertDoesNotThrow(() -> context.verifier().verify("token", "203.0.113.7"));
    }

    private static TestContext context(boolean failOpen) {
        return context(failOpen, List.of(HOSTNAME));
    }

    private static TestContext context(boolean failOpen, List<String> hostnames) {
        CaptchaProperties properties = new CaptchaProperties(
                true,
                VERIFY_URL,
                "site",
                "secret",
                "login",
                hostnames,
                failOpen,
                Duration.ofSeconds(2),
                Duration.ofSeconds(3)
        );
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        return new TestContext(builder.build(), server, properties);
    }

    private record TestContext(
            RestClient restClient,
            MockRestServiceServer server,
            CaptchaProperties properties
    ) {
        TurnstileCaptchaVerifier verifier() {
            return new TurnstileCaptchaVerifier(restClient, properties);
        }
    }
}
