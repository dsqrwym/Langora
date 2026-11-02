package org.dsqrwym.langora.infrastructure.security.login.store;

import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;

@Slf4j
public class FallbackLoginAttemptStore implements LoginAttemptStore {
    private static final long WARN_INTERVAL_NANOS = Duration.ofMinutes(1).toNanos();
    private final LoginAttemptStore primary;
    private final LoginAttemptStore fallback;
    private final long cooldownNanos;
    private final LongSupplier nanoClock;
    // 降级截止时刻
    private final AtomicLong degradedUntilNanos = new AtomicLong();
    // 上次告警时刻
    private final AtomicLong lastWarnNanos = new AtomicLong(Long.MIN_VALUE);

    public FallbackLoginAttemptStore(LoginAttemptStore primary, LoginAttemptStore fallback, Duration cooldown) {
        this(primary, fallback, cooldown, System::nanoTime);
    }

    FallbackLoginAttemptStore(
            LoginAttemptStore primary, LoginAttemptStore fallback, Duration cooldown, LongSupplier nanoClock) {
        this.primary = primary;
        this.fallback = fallback;
        this.cooldownNanos = cooldown.toNanos();
        this.nanoClock = nanoClock;
    }

    private boolean isDegraded() {
        final long until = degradedUntilNanos.get();
        return until != 0L && nanoClock.getAsLong() - until < 0L;
    }

    private void degrade(String operation, RuntimeException ex) {
        final long now = nanoClock.getAsLong();
        degradedUntilNanos.set(now + cooldownNanos);

        final long last = lastWarnNanos.get();
        final boolean due = last == Long.MIN_VALUE || now - last >= WARN_INTERVAL_NANOS;
        if (due && lastWarnNanos.compareAndSet(last, now)) {
            log.warn("[LoginAttemptStore] Redis {} failed; falling back to the in-memory store for {}: {}",
                    operation, Duration.ofNanos(cooldownNanos), ex.toString());
        }
    }

    @Override
    public AttemptState inspect(String key) {
        if (isDegraded()) {
            return fallback.inspect(key);
        }
        try {
            return primary.inspect(key);
        } catch (RuntimeException ex) {
            degrade("inspect", ex);
            return fallback.inspect(key);
        }
    }

    @Override
    public AttemptState increment(String key, Duration window) {
        if (isDegraded()) {
            return fallback.increment(key, window);
        }
        try {
            return primary.increment(key, window);
        } catch (RuntimeException ex) {
            degrade("increment", ex);
            return fallback.increment(key, window);
        }
    }

    @Override
    public void reset(String key) {
        try {
            primary.reset(key);
        } catch (RuntimeException ex) {
            degrade("reset", ex);
        }
        fallback.reset(key);
    }
}
