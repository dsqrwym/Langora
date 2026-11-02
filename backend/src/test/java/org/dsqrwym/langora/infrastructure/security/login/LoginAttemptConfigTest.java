package org.dsqrwym.langora.infrastructure.security.login;

import org.dsqrwym.langora.infrastructure.security.login.store.FallbackLoginAttemptStore;
import org.dsqrwym.langora.infrastructure.security.login.store.LoginAttemptStore;
import org.dsqrwym.langora.infrastructure.security.login.store.MemoryLoginAttemptStore;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Verifies the store selection: memory by default, Redis + memory fallback when configured.
 */
class LoginAttemptConfigTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(LoginAttemptConfig.class, PropertiesConfiguration.class);

    @Test
    void defaultsToTheMemoryStore() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBean(LoginAttemptStore.class))
                    .isInstanceOf(MemoryLoginAttemptStore.class);
            assertThat(context.getBean(LoginAttemptService.class)).isNotNull();
        });
    }

    @Test
    void redisStoreIsWrappedWithTheMemoryFallback() {
        runner.withPropertyValues("langora.security.login-attempt.store=redis")
                .withBean(StringRedisTemplate.class, () -> mock(StringRedisTemplate.class))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(LoginAttemptStore.class))
                            .isInstanceOf(FallbackLoginAttemptStore.class);
                });
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(LoginAttemptProperties.class)
    static class PropertiesConfiguration {
    }
}
