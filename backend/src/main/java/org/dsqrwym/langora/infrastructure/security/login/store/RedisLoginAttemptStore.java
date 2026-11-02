package org.dsqrwym.langora.infrastructure.security.login.store;

import java.time.Duration;
import java.util.List;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

public class RedisLoginAttemptStore implements LoginAttemptStore {
    private static final String KEY_PREFIX = "langora:login_attempt:";
    private static final DefaultRedisScript<String> INCREMENT_SCRIPT = new DefaultRedisScript<>("""
            local count = redis.call('incr',KEYS[1])
            local ttl = redis.call('pttl',KEYS[1])
            if ttl < 0 then
                ttl = tonumber(ARGV[1])
                redis.call('pexpire',KEYS[1],ttl)
            end
            return tostring(count) .. ':' .. tostring(ttl)
            """, String.class);

    private static final DefaultRedisScript<String> INSPECT_SCRIPT = new DefaultRedisScript<>("""
            local count = redis.call('get',KEYS[1])
            if not count then
                return nil
            end
            return count .. ':' .. tostring(redis.call('pttl',KEYS[1]))
            """, String.class);

    private final StringRedisTemplate redis;

    public RedisLoginAttemptStore(StringRedisTemplate redis) {
        this.redis = redis;
    }

    private static AttemptState parse(String payload) {
        if (payload == null || payload.isEmpty()) {
            return AttemptState.EMPTY;
        }
        final int separator = payload.indexOf(':');
        if (separator < 0) {
            throw new IllegalStateException("Unexpected login counter payload: " + payload);
        }
        final long count = Long.parseLong(payload.substring(0, separator));
        final long ttlMillis = Long.parseLong(payload.substring(separator + 1));
        // PTTL 对 存在但无过期 的 key 返回 -1，当作 0 处理，不要产生负的 Duration
        return new AttemptState(count, Duration.ofMillis(Math.max(0L, ttlMillis)));
    }

    @Override
    public AttemptState inspect(String key) {
        return parse(redis.execute(INSPECT_SCRIPT, List.of(KEY_PREFIX + key)));
    }

    @Override
    public AttemptState increment(String key, Duration window) {
        return parse(redis.execute(
                INCREMENT_SCRIPT,
                List.of(KEY_PREFIX + key),
                // 0 will remove the key
                Long.toString(Math.max(1L, window.toMillis()))
        ));
    }

    @Override
    public void reset(String key) {
        redis.delete(KEY_PREFIX + key);
    }
}
