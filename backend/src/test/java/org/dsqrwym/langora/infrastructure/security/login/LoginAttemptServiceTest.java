package org.dsqrwym.langora.infrastructure.security.login;

import org.dsqrwym.langora.auth.UserTypes;
import org.dsqrwym.langora.infrastructure.security.login.store.LoginAttemptStore;
import org.dsqrwym.langora.infrastructure.web.exception.RateLimitedException;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the two login-attempt dimensions: IP (CAPTCHA + hard block) and account.
 */
class LoginAttemptServiceTest {

    private static final Duration WINDOW = Duration.ofMinutes(15);

    @Test
    void ipRequiresCaptchaAtThresholdButNotBelowIt() {
        LoginAttemptService service = service();

        assertFalse(service.checkIp("203.0.113.7"));
        service.recordIpFailure("203.0.113.7");
        service.recordIpFailure("203.0.113.7");
        assertFalse(service.checkIp("203.0.113.7"));

        service.recordIpFailure("203.0.113.7");
        assertTrue(service.checkIp("203.0.113.7"));
    }

    @Test
    void ipHardBlocksAtTheIpThreshold() {
        LoginAttemptService service = service();
        for (int i = 0; i < 20; i++) {
            service.recordIpFailure("203.0.113.7");
        }

        RateLimitedException exception = assertThrows(RateLimitedException.class,
                () -> service.checkIp("203.0.113.7"));

        assertEquals(429, exception.status.value());
        assertEquals(900, exception.retryAfterSeconds);
    }

    @Test
    void accountHardBlocksAtTheAccountThreshold() {
        LoginAttemptService service = service();
        for (int i = 0; i < 4; i++) {
            service.recordAccountFailure(adminKey(7));
        }
        assertDoesNotThrow(() -> service.checkAccount(adminKey(7)));

        service.recordAccountFailure(adminKey(7));
        RateLimitedException exception = assertThrows(RateLimitedException.class,
                () -> service.checkAccount(adminKey(7)));

        assertEquals(429, exception.status.value());
        assertEquals(900, exception.retryAfterSeconds);
    }

    @Test
    void successfulLoginClearsAccountButKeepsIpCounter() {
        LoginAttemptService service = service();
        service.recordAccountFailure(adminKey(7));
        service.recordIpFailure("203.0.113.7");
        service.recordIpFailure("203.0.113.7");
        service.recordIpFailure("203.0.113.7");

        service.clearAccount(adminKey(7));

        assertDoesNotThrow(() -> service.checkAccount(adminKey(7)));
        assertTrue(service.checkIp("203.0.113.7"));
    }

    @Test
    void nullAndBlankIpShareTheUnknownBucket() {
        LoginAttemptService service = service();
        service.recordIpFailure(null);
        service.recordIpFailure("");
        service.recordIpFailure("   ");

        assertTrue(service.checkIp(null));
        assertTrue(service.checkIp(""));
    }

    @Test
    void accountCountersAreNamespacedByType() {
        LoginAttemptService service = service();
        LoginAttemptAccountKey<UserTypes> admin7 = LoginAttemptAccountKey.of(UserTypes.ADMIN, "7");
        LoginAttemptAccountKey<UserTypes> teacher7 = LoginAttemptAccountKey.of(UserTypes.TEACHER, "7");

        for (int i = 0; i < 5; i++) {
            service.recordAccountFailure(admin7);
        }

        assertThrows(RateLimitedException.class, () -> service.checkAccount(adminKey(7)));
        assertDoesNotThrow(() -> service.checkAccount(teacher7));

        service.clearAccount(adminKey(7));
        assertDoesNotThrow(() -> service.checkAccount(admin7));
        assertDoesNotThrow(() -> service.checkAccount(teacher7));
    }

    @Test
    void countersAreIsolatedPerIpAddress() {
        LoginAttemptService service = service();
        service.recordIpFailure("198.51.100.1");
        service.recordIpFailure("198.51.100.1");
        service.recordIpFailure("198.51.100.1");

        assertTrue(service.checkIp("198.51.100.1"));
        assertFalse(service.checkIp("198.51.100.2"));
    }

    private static LoginAttemptService service() {
        LoginAttemptProperties properties = new LoginAttemptProperties(
                LoginAttemptProperties.Store.MEMORY,
                20,
                3,
                5,
                WINDOW,
                Duration.ofMinutes(5),
                1_000,
                Duration.ofSeconds(30)
        );
        return new LoginAttemptService(new FakeStore(), properties);
    }

    private static LoginAttemptAccountKey<UserTypes> adminKey(long userId) {
        return LoginAttemptAccountKey.of(UserTypes.ADMIN, Long.toString(userId));
    }

    /**
     * Minimal deterministic store: no TTL, but the service tests only inspect/increment/reset.
     */
    private static final class FakeStore implements LoginAttemptStore {
        private final Map<String, AttemptState> states = new HashMap<>();

        @Override
        public AttemptState inspect(String key) {
            return states.getOrDefault(key, AttemptState.EMPTY);
        }

        @Override
        public AttemptState increment(String key, Duration window) {
            AttemptState current = states.get(key);
            long nextCount = current == null ? 1L : current.count() + 1L;
            AttemptState next = new AttemptState(nextCount, window);
            states.put(key, next);
            return next;
        }

        @Override
        public void reset(String key) {
            states.remove(key);
        }
    }
}
