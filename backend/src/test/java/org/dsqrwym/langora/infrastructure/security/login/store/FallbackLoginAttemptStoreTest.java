package org.dsqrwym.langora.infrastructure.security.login.store;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

/**
 * The Redis-backed store is primary; the in-memory store must keep login working while Redis is down.
 */
class FallbackLoginAttemptStoreTest {

    private static final Duration WINDOW = Duration.ofMinutes(15);
    private static final Duration COOLDOWN = Duration.ofSeconds(30);

    @Test
    void usesPrimaryWhenItIsHealthy() {
        LoginAttemptStore primary = mock(LoginAttemptStore.class);
        LoginAttemptStore fallback = mock(LoginAttemptStore.class);
        FallbackLoginAttemptStore store = new FallbackLoginAttemptStore(
                primary, fallback, COOLDOWN, () -> 1_000L);
        LoginAttemptStore.AttemptState state = new LoginAttemptStore.AttemptState(2L, Duration.ofSeconds(5));
        given(primary.inspect("account:7")).willReturn(state);

        assertSame(state, store.inspect("account:7"));

        then(fallback).shouldHaveNoInteractions();
    }

    @Test
    void fallsBackOnPrimaryFailureAndStaysDegradedDuringCooldown() {
        AtomicLong clock = new AtomicLong(1_000L);
        LoginAttemptStore primary = mock(LoginAttemptStore.class);
        LoginAttemptStore fallback = mock(LoginAttemptStore.class);
        FallbackLoginAttemptStore store = new FallbackLoginAttemptStore(
                primary, fallback, COOLDOWN, clock::get);
        LoginAttemptStore.AttemptState fallbackState =
                new LoginAttemptStore.AttemptState(3L, Duration.ofSeconds(10));
        given(primary.inspect("account:7")).willThrow(new IllegalStateException("redis unavailable"));
        given(fallback.inspect("account:7")).willReturn(fallbackState);

        assertSame(fallbackState, store.inspect("account:7"));
        clock.addAndGet(COOLDOWN.toNanos() - 1);
        assertSame(fallbackState, store.inspect("account:7"));

        then(primary).should(times(1)).inspect("account:7");
        then(fallback).should(times(2)).inspect("account:7");
    }

    @Test
    void retriesPrimaryAfterCooldownExpires() {
        AtomicLong clock = new AtomicLong(1_000L);
        LoginAttemptStore primary = mock(LoginAttemptStore.class);
        LoginAttemptStore fallback = mock(LoginAttemptStore.class);
        FallbackLoginAttemptStore store = new FallbackLoginAttemptStore(
                primary, fallback, COOLDOWN, clock::get);
        LoginAttemptStore.AttemptState primaryState =
                new LoginAttemptStore.AttemptState(1L, Duration.ofSeconds(5));
        LoginAttemptStore.AttemptState fallbackState =
                new LoginAttemptStore.AttemptState(99L, Duration.ofSeconds(5));
        given(primary.inspect("account:7"))
                .willThrow(new IllegalStateException("redis unavailable"))
                .willReturn(primaryState);
        given(fallback.inspect("account:7")).willReturn(fallbackState);

        assertSame(fallbackState, store.inspect("account:7"));
        clock.addAndGet(COOLDOWN.toNanos() + 1);
        assertSame(primaryState, store.inspect("account:7"));

        then(primary).should(times(2)).inspect("account:7");
    }

    @Test
    void incrementAlsoFallsBack() {
        LoginAttemptStore primary = mock(LoginAttemptStore.class);
        LoginAttemptStore fallback = mock(LoginAttemptStore.class);
        FallbackLoginAttemptStore store = new FallbackLoginAttemptStore(
                primary, fallback, COOLDOWN, () -> 1_000L);
        LoginAttemptStore.AttemptState fallbackState =
                new LoginAttemptStore.AttemptState(4L, WINDOW);
        given(primary.increment("account:7", WINDOW)).willThrow(new IllegalStateException("redis unavailable"));
        given(fallback.increment("account:7", WINDOW)).willReturn(fallbackState);

        assertSame(fallbackState, store.increment("account:7", WINDOW));

        then(primary).should().increment("account:7", WINDOW);
        then(fallback).should().increment("account:7", WINDOW);
    }

    @Test
    void resetClearsBothStoresWhenPrimaryIsHealthy() {
        LoginAttemptStore primary = mock(LoginAttemptStore.class);
        LoginAttemptStore fallback = mock(LoginAttemptStore.class);
        FallbackLoginAttemptStore store = new FallbackLoginAttemptStore(
                primary, fallback, COOLDOWN, () -> 1_000L);

        store.reset("account:7");

        then(primary).should().reset("account:7");
        then(fallback).should().reset("account:7");
    }

    @Test
    void resetAttemptsPrimaryEvenWhileDegraded() {
        LoginAttemptStore primary = mock(LoginAttemptStore.class);
        LoginAttemptStore fallback = mock(LoginAttemptStore.class);
        FallbackLoginAttemptStore store = new FallbackLoginAttemptStore(
                primary, fallback, COOLDOWN, () -> 1_000L);
        LoginAttemptStore.AttemptState state =
                new LoginAttemptStore.AttemptState(4L, Duration.ofSeconds(5));
        given(primary.inspect("account:admin:7"))
                .willThrow(new IllegalStateException("redis unavailable"));
        given(fallback.inspect("account:admin:7")).willReturn(state);

        assertSame(state, store.inspect("account:admin:7"));
        store.reset("account:admin:7");

        then(primary).should().reset("account:admin:7");
        then(fallback).should().reset("account:admin:7");
    }

    @Test
    void fallbackInspectResultIsReturnedVerbatim() {
        LoginAttemptStore primary = mock(LoginAttemptStore.class);
        LoginAttemptStore fallback = mock(LoginAttemptStore.class);
        FallbackLoginAttemptStore store = new FallbackLoginAttemptStore(
                primary, fallback, COOLDOWN, () -> 1_000L);
        LoginAttemptStore.AttemptState fallbackState =
                new LoginAttemptStore.AttemptState(7L, Duration.ofSeconds(1));
        given(primary.inspect("ip:203.0.113.7")).willThrow(new IllegalStateException("redis unavailable"));
        given(fallback.inspect("ip:203.0.113.7")).willReturn(fallbackState);

        assertEquals(7L, store.inspect("ip:203.0.113.7").count());
    }
}
