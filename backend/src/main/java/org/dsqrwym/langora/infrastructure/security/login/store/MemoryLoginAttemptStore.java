package org.dsqrwym.langora.infrastructure.security.login.store;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.LongSupplier;

@Slf4j
public class MemoryLoginAttemptStore implements LoginAttemptStore {
    private record Window(long count, long expiresAtNanos) {
        boolean isExpired(long now) {
            return now >= expiresAtNanos;
        }

        Window withCount(long newCount) {
            return new Window(newCount, expiresAtNanos);
        }

        LoginAttemptStore.AttemptState toState(long now) {
            return new LoginAttemptStore.AttemptState(count, Duration.ofNanos(Math.max(0L, expiresAtNanos - now)));
        }
    }

    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();
    private final LongSupplier nanoClock;
    private final int maxLocalEntries;
    private final AtomicInteger droppedKeys = new AtomicInteger();

    // nanoClock 用于单元测试
    MemoryLoginAttemptStore(int maxLocalEntries, LongSupplier nanoClock) {
        this.maxLocalEntries = maxLocalEntries;
        this.nanoClock = nanoClock;
    }

    public MemoryLoginAttemptStore(int maxLocalEntries) {
        this(maxLocalEntries, System::nanoTime);
    }

    @Override
    public AttemptState inspect(String key) {
        final long now = nanoClock.getAsLong();
        final Window current = windows.get(key);
        if (current == null) {
            return AttemptState.EMPTY;
        }
        // 顺便主动删除
        if (current.isExpired(now)) {
            windows.remove(key, current);
            return AttemptState.EMPTY;
        }
        return current.toState(now);
    }

    @Override
    public AttemptState increment(String key, Duration window) {
        final long now = nanoClock.getAsLong();
        // 高于上限丢弃 key, 包是
        if (!windows.containsKey(key) && !hasCapacity()) {
            return AttemptState.EMPTY;
        }

        // atomic update
        final Window updated = windows.compute(key, (ignored, current) -> {
            if (current == null || current.isExpired(now)) {
                return new Window(1L, now + window.toNanos());
            }
            return current.withCount(current.count() + 1L);
        });

        return updated.toState(now);
    }

    @Override
    public void reset(String Key) {
        windows.remove(Key);
    }

    @Scheduled(fixedDelayString = "${langora.security.login-attempt.cleanup-interval:5m}")
    public void evictExpired() {
        final long now = nanoClock.getAsLong();
        final int before = windows.size();
        for (Map.Entry<String, Window> entry : windows.entrySet()) {
            if (entry.getValue().isExpired(now)) {
                windows.remove(entry.getKey(), entry.getValue());
            }
        }
        final int removed = before - windows.size();
        if (removed > 0) {
            log.debug("[LoginAttemptStore] Evicted {} expired window(s), {} remaining", removed, windows.size());
        }
    }

    private boolean hasCapacity() {
        if (windows.size() < maxLocalEntries) {
            return true;
        }
        // 满了就清理一次
        evictExpired();
        if (windows.size() < maxLocalEntries) {
            return true;
        }
        // 大规模攻击，每一千次打一次日志
        if (droppedKeys.incrementAndGet() % 1000 == 1) {
            log.warn("[LoginAttemptStore] Capacity {} reached; dropping new login counters ({} dropped so far). Raise langora.security.login-attempt.max-local-entries or switch to the Redis store.", maxLocalEntries, droppedKeys.get());
        }

        return false;
    }
}
