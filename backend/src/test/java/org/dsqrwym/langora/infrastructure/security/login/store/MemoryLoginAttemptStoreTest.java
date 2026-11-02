package org.dsqrwym.langora.infrastructure.security.login.store;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Fixed-window and capacity behaviour of the default in-memory store.
 */
class MemoryLoginAttemptStoreTest {

    @Test
    void incrementsWithinOneFixedWindowAndStartsANewWindowAfterExpiry() {
        AtomicLong clock = new AtomicLong(1_000L);
        MemoryLoginAttemptStore store = new MemoryLoginAttemptStore(10, clock::get);

        LoginAttemptStore.AttemptState first = store.increment("account:7", Duration.ofSeconds(10));
        assertEquals(1L, first.count());
        assertEquals(10L, first.retryAfter().toSeconds());

        assertEquals(2L, store.increment("account:7", Duration.ofSeconds(10)).count());

        clock.addAndGet(Duration.ofSeconds(10).toNanos());

        assertEquals(0L, store.inspect("account:7").count());
        assertEquals(1L, store.increment("account:7", Duration.ofSeconds(10)).count());
    }

    @Test
    void resetRemovesTheCounter() {
        MemoryLoginAttemptStore store = new MemoryLoginAttemptStore(10, System::nanoTime);
        store.increment("account:7", Duration.ofMinutes(15));

        store.reset("account:7");

        assertEquals(0L, store.inspect("account:7").count());
    }

    @Test
    void dropsNewKeysWhenCapacityIsFullButStillCountsExistingKeys() {
        MemoryLoginAttemptStore store = new MemoryLoginAttemptStore(2, System::nanoTime);
        store.increment("ip:a", Duration.ofMinutes(15));
        store.increment("ip:b", Duration.ofMinutes(15));

        assertEquals(0L, store.increment("ip:c", Duration.ofMinutes(15)).count());
        assertEquals(0L, store.inspect("ip:c").count());

        assertEquals(2L, store.increment("ip:a", Duration.ofMinutes(15)).count());

        store.reset("ip:b");
        assertEquals(1L, store.increment("ip:c", Duration.ofMinutes(15)).count());
    }

    @Test
    void evictExpiredReclaimsCapacity() {
        AtomicLong clock = new AtomicLong(1_000L);
        MemoryLoginAttemptStore store = new MemoryLoginAttemptStore(1, clock::get);

        store.increment("ip:a", Duration.ofSeconds(5));
        assertEquals(0L, store.increment("ip:b", Duration.ofSeconds(5)).count());

        clock.addAndGet(Duration.ofSeconds(5).toNanos());
        store.evictExpired();

        assertEquals(1L, store.increment("ip:b", Duration.ofSeconds(5)).count());
    }
}
