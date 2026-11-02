package org.dsqrwym.langora.infrastructure.security.login;

import org.dsqrwym.langora.infrastructure.security.login.store.FallbackLoginAttemptStore;
import org.dsqrwym.langora.infrastructure.security.login.store.LoginAttemptStore;
import org.dsqrwym.langora.infrastructure.security.login.store.MemoryLoginAttemptStore;
import org.dsqrwym.langora.infrastructure.security.login.store.RedisLoginAttemptStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration(proxyBeanMethods = false)
public class LoginAttemptConfig {
    @Bean
    public MemoryLoginAttemptStore memoryLoginAttemptStore(LoginAttemptProperties properties) {
        return new MemoryLoginAttemptStore(properties.maxLocalEntries());
    }

    @Bean
    @ConditionalOnProperty(prefix = "langora.security.login-attempt", name = "store", havingValue = "redis")
    public RedisLoginAttemptStore redisLoginAttemptStore(StringRedisTemplate redis) {
        return new RedisLoginAttemptStore(redis);
    }

    /**
     * 对外暴露的 store
     */
    @Bean
    @Primary
    public LoginAttemptStore loginAttemptStore(
            MemoryLoginAttemptStore memoryStore,
            ObjectProvider<RedisLoginAttemptStore> redisStore,
            LoginAttemptProperties properties
    ) {
        final RedisLoginAttemptStore redis = redisStore.getIfAvailable();
        if (redis == null) {
            return memoryStore;
        }
        return new FallbackLoginAttemptStore(redis, memoryStore, properties.redisDegradedCooldown());
    }

    @Bean
    public LoginAttemptService loginAttemptService(LoginAttemptStore store, LoginAttemptProperties properties) {
        return new LoginAttemptService(store, properties);
    }
}
