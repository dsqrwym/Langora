package org.dsqrwym.langora.infrastructure.resilience.jooq;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import org.dsqrwym.langora.infrastructure.web.exception.RateLimitedException;
import org.jooq.ConnectionProvider;
import org.jooq.exception.DataAccessException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.sql.Connection;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

/**
 * Permit pairing for the statement-level gate: every {@code acquire()} that succeeds must be
 * matched by exactly one {@code release()}, and every rejection must leave the permit count intact.
 */
class BulkheadConnectionProviderTest {

    private final ConnectionProvider delegate = mock(ConnectionProvider.class);

    @AfterEach
    void clearTransactionState() {
        TransactionSynchronizationManager.clear();
    }

    private static Bulkhead bulkhead(int permits) {
        return Bulkhead.of("test", BulkheadConfig.custom()
                .maxConcurrentCalls(permits)
                .maxWaitDuration(Duration.ZERO)
                .build());
    }

    @Test
    void holdsOnePermitUntilTheConnectionIsReleased() {
        Bulkhead bulkhead = bulkhead(1);
        Connection connection = mock(Connection.class);
        given(delegate.acquire()).willReturn(connection);
        BulkheadConnectionProvider provider = new BulkheadConnectionProvider(delegate, bulkhead);

        assertThat(provider.acquire()).isSameAs(connection);
        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isZero();

        provider.release(connection);

        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(1);
        then(delegate).should().release(connection);
    }

    @Test
    void rejectsWithoutTouchingTheDelegate() {
        Bulkhead bulkhead = bulkhead(1);
        assertThat(bulkhead.tryAcquirePermission()).isTrue();
        BulkheadConnectionProvider provider = new BulkheadConnectionProvider(delegate, bulkhead);

        assertThatThrownBy(provider::acquire).isInstanceOf(RateLimitedException.class);

        then(delegate).shouldHaveNoInteractions();
        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isZero();
    }

    @Test
    void returnsThePermitWhenTheDelegateYieldsNoConnection() {
        Bulkhead bulkhead = bulkhead(1);
        given(delegate.acquire()).willReturn((Connection) null);
        BulkheadConnectionProvider provider = new BulkheadConnectionProvider(delegate, bulkhead);

        assertThat(provider.acquire()).isNull();
        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(1);
    }

    @Test
    void returnsThePermitWhenTheDelegateThrows() {
        Bulkhead bulkhead = bulkhead(1);
        given(delegate.acquire()).willThrow(new DataAccessException("no connection"));
        BulkheadConnectionProvider provider = new BulkheadConnectionProvider(delegate, bulkhead);

        assertThatThrownBy(provider::acquire).isInstanceOf(DataAccessException.class);
        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(1);
    }

    @Test
    void leavesThePermitToTheTransactionInsideAnActiveTransaction() {
        Bulkhead bulkhead = bulkhead(1);
        Connection connection = mock(Connection.class);
        given(delegate.acquire()).willReturn(connection);
        BulkheadConnectionProvider provider = new BulkheadConnectionProvider(delegate, bulkhead);

        TransactionSynchronizationManager.setActualTransactionActive(true);
        try {
            assertThat(provider.acquire()).isSameAs(connection);
            // The transaction owns the permit; a statement must not consume a second one.
            assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(1);
        } finally {
            TransactionSynchronizationManager.setActualTransactionActive(false);
        }
    }

    @Test
    void doesNotReturnAPermitWhenAStatementCompletesInsideATransaction() {
        Bulkhead bulkhead = bulkhead(1);
        Connection connection = mock(Connection.class);
        given(delegate.acquire()).willReturn(connection);
        BulkheadConnectionProvider provider = new BulkheadConnectionProvider(delegate, bulkhead);

        TransactionSynchronizationManager.setActualTransactionActive(true);
        try {
            provider.release(provider.acquire());
        } finally {
            TransactionSynchronizationManager.setActualTransactionActive(false);
        }

        // Regression: release() used to call onComplete() unconditionally, so this was 2.
        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(1);
    }

    @Test
    void manyStatementsInsideOneTransactionLeaveThePermitCountUnchanged() {
        Bulkhead bulkhead = bulkhead(2);
        Connection connection = mock(Connection.class);
        given(delegate.acquire()).willReturn(connection);
        BulkheadConnectionProvider provider = new BulkheadConnectionProvider(delegate, bulkhead);

        TransactionSynchronizationManager.setActualTransactionActive(true);
        try {
            for (int statement = 0; statement < 100; statement++) {
                provider.release(provider.acquire());
            }
        } finally {
            TransactionSynchronizationManager.setActualTransactionActive(false);
        }

        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(2);
    }

    @Test
    void keepsRejectingAfterManyStatementsInsideTransactions() {
        Bulkhead bulkhead = bulkhead(1);
        Connection connection = mock(Connection.class);
        given(delegate.acquire()).willReturn(connection);
        BulkheadConnectionProvider provider = new BulkheadConnectionProvider(delegate, bulkhead);

        TransactionSynchronizationManager.setActualTransactionActive(true);
        try {
            for (int statement = 0; statement < 100; statement++) {
                provider.release(provider.acquire());
            }
        } finally {
            TransactionSynchronizationManager.setActualTransactionActive(false);
        }

        // The single permit is still the only one in existence.
        assertThat(bulkhead.tryAcquirePermission()).isTrue();
        assertThatThrownBy(provider::acquire).isInstanceOf(RateLimitedException.class);
    }

    @Test
    void returnsOnlyOnePermitWhenTheSameConnectionIsReleasedTwice() {
        Bulkhead bulkhead = bulkhead(1);
        Connection connection = mock(Connection.class);
        given(delegate.acquire()).willReturn(connection);
        BulkheadConnectionProvider provider = new BulkheadConnectionProvider(delegate, bulkhead);

        Connection acquired = provider.acquire();
        provider.release(acquired);
        provider.release(acquired);

        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(1);
        then(delegate).should(times(2)).release(connection);
    }

    @Test
    void returnsThePermitEvenWhenTheConnectionIsReleasedAfterTheTransactionEnded() {
        Bulkhead bulkhead = bulkhead(2);
        Connection connection = mock(Connection.class);
        given(delegate.acquire()).willReturn(connection);
        BulkheadConnectionProvider provider = new BulkheadConnectionProvider(delegate, bulkhead);

        Connection acquired = provider.acquire();
        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(1);

        TransactionSynchronizationManager.setActualTransactionActive(true);
        try {
            provider.release(acquired);
        } finally {
            TransactionSynchronizationManager.setActualTransactionActive(false);
        }

        // The ledger, not the ambient transaction state, decides whether a permit is returned. An
        // implementation that mirrors the acquire-side transaction check would lose this permit.
        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(2);
    }

    @Test
    void publishesOneFinishedEventForEveryPermittedEvent() {
        Bulkhead bulkhead = bulkhead(4);
        Connection connection = mock(Connection.class);
        given(delegate.acquire()).willReturn(connection);
        BulkheadConnectionProvider provider = new BulkheadConnectionProvider(delegate, bulkhead);

        AtomicInteger permitted = new AtomicInteger();
        AtomicInteger finished = new AtomicInteger();
        bulkhead.getEventPublisher()
                .onCallPermitted(_ -> permitted.incrementAndGet())
                .onCallFinished(_ -> finished.incrementAndGet());

        provider.release(provider.acquire());
        provider.release(provider.acquire());

        TransactionSynchronizationManager.setActualTransactionActive(true);
        try {
            for (int statement = 0; statement < 5; statement++) {
                provider.release(provider.acquire());
            }
        } finally {
            TransactionSynchronizationManager.setActualTransactionActive(false);
        }

        assertThat(permitted).hasValue(2);
        assertThat(finished).hasValue(2);
    }
}
