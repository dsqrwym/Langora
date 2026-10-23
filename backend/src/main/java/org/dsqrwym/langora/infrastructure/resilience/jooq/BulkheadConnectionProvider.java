package org.dsqrwym.langora.infrastructure.resilience.jooq;

import io.github.resilience4j.bulkhead.Bulkhead;
import org.dsqrwym.langora.infrastructure.web.exception.RateLimitedException;
import org.jooq.ConnectionProvider;
import org.jooq.exception.DataAccessException;
import org.jspecify.annotations.Nullable;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.sql.Connection;

/**
 * BulkheadConnectionProvider
 * <p>
 * Database admission control: A single shared resilience4j bulkhead guards all SQL executions,
 * where permits are acquired only on active SQL issuance and released on active SQL completion.
 * <p>
 * jOOQ contract: {@code acquire()} is called once per {@code ExecuteContext},
 * and {@code release(Connection)} is guaranteed to be called once to match it,
 * so permit pairing is guaranteed by jOOQ.
 */
public class BulkheadConnectionProvider implements ConnectionProvider {
    private final ConnectionProvider delegate;
    private final Bulkhead bulkhead;

    public BulkheadConnectionProvider(ConnectionProvider delegate, Bulkhead bulkhead) {
        this.delegate = delegate;
        this.bulkhead = bulkhead;
    }

    @Override
    public @Nullable Connection acquire() throws DataAccessException {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            return delegate.acquire();
        }
        if (!bulkhead.tryAcquirePermission()) {
            // Waited maxWaitDuration and still no permit: reject before the pool is touched.
            throw new RateLimitedException(bulkhead.getBulkheadConfig().getMaxWaitDuration());
        }

        boolean handed = false;
        try {
            Connection connection = delegate.acquire();
            handed = connection != null;
            return connection;
        } finally {
            if (!handed) {
                bulkhead.onComplete();
            }
        }
    }

    @Override
    public void release(Connection connection) throws DataAccessException {
        try {
            delegate.release(connection);
        } finally {
            bulkhead.onComplete();
        }
    }
}
