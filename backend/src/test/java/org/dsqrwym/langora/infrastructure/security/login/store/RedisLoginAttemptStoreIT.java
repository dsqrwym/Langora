package org.dsqrwym.langora.infrastructure.security.login.store;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises the real Lua scripts against a disposable Redis. Skipped when Docker is unavailable.
 */
@Testcontainers(disabledWithoutDocker = true)
class RedisLoginAttemptStoreIT {

    @Container
    static final GenericContainer<?> REDIS =
            new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    private static LettuceConnectionFactory factory;
    private static RedisLoginAttemptStore store;

    @BeforeAll
    static void setUp() {
        factory = new LettuceConnectionFactory(REDIS.getHost(), REDIS.getMappedPort(6379));
        factory.afterPropertiesSet();
        StringRedisTemplate template = new StringRedisTemplate(factory);
        template.afterPropertiesSet();
        store = new RedisLoginAttemptStore(template);
    }

    @AfterAll
    static void tearDown() {
        if (factory != null) {
            factory.destroy();
        }
    }

    @Test
    void incrementsInspectsAndResets() {
        Duration window = Duration.ofMinutes(15);

        LoginAttemptStore.AttemptState first = store.increment("account:1", window);
        assertEquals(1L, first.count());

        LoginAttemptStore.AttemptState second = store.increment("account:1", window);
        assertEquals(2L, second.count());
        assertTrue(second.retryAfter().toSeconds() > 0);

        assertEquals(2L, store.inspect("account:1").count());

        store.reset("account:1");
        assertEquals(0L, store.inspect("account:1").count());
    }
}
