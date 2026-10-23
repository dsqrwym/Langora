package org.dsqrwym.langora.infrastructure.resilience.tx;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import org.dsqrwym.langora.infrastructure.web.exception.RateLimitedException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * The permit is taken at the transaction boundary, i.e. before {@code doBegin()} asks the pool for
 * a connection. Uses a real {@link DataSourceTransactionManager} so that
 * {@link TransactionSynchronizationManager#isActualTransactionActive()} behaves like production.
 */
class BulkheadTransactionTemplateTest {

    private final DataSource dataSource = mock(DataSource.class);

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

    private BulkheadTransactionTemplate template(Bulkhead bulkhead) {
        return new BulkheadTransactionTemplate(new DataSourceTransactionManager(dataSource), bulkhead);
    }

    @Test
    void rejectsBeforeAnyConnectionIsTaken() {
        Bulkhead bulkhead = bulkhead(1);
        assertThat(bulkhead.tryAcquirePermission()).isTrue();
        BulkheadTransactionTemplate tx = template(bulkhead);

        assertThatThrownBy(() -> tx.executeWithoutResult(_ -> { }))
                .isInstanceOf(RateLimitedException.class)
                .hasMessage("Too many requests");

        // Rejection happens before the pool is ever asked for a connection.
        verifyNoInteractions(dataSource);
    }

    @Test
    void releasesThePermitAfterCommit() throws Exception {
        Connection connection = mock(Connection.class);
        given(dataSource.getConnection()).willReturn(connection);
        Bulkhead bulkhead = bulkhead(1);
        BulkheadTransactionTemplate tx = template(bulkhead);

        tx.executeWithoutResult(_ -> { });

        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(1);
        verify(connection).commit();
    }

    @Test
    void releasesThePermitAfterRollback() throws Exception {
        Connection connection = mock(Connection.class);
        given(dataSource.getConnection()).willReturn(connection);
        Bulkhead bulkhead = bulkhead(1);
        BulkheadTransactionTemplate tx = template(bulkhead);

        assertThatThrownBy(() -> tx.executeWithoutResult(_ -> {
            throw new IllegalStateException("boom");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(1);
        verify(connection).rollback();
    }

    @Test
    void nestedExecutionReusesTheOuterPermit() throws Exception {
        given(dataSource.getConnection()).willReturn(mock(Connection.class));
        Bulkhead bulkhead = bulkhead(2);
        BulkheadTransactionTemplate tx = template(bulkhead);

        tx.executeWithoutResult(_ -> {
            assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(1);

            tx.executeWithoutResult(_ ->
                    assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(1));
        });

        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(2);
        verify(dataSource, times(1)).getConnection();
    }
}
