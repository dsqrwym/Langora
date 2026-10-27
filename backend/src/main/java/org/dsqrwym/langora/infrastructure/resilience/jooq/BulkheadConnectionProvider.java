package org.dsqrwym.langora.infrastructure.resilience.jooq;

import io.github.resilience4j.bulkhead.Bulkhead;
import org.dsqrwym.langora.infrastructure.web.exception.RateLimitedException;
import org.jooq.ConnectionProvider;
import org.jooq.exception.DataAccessException;
import org.jspecify.annotations.Nullable;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.sql.Connection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * BulkheadConnectionProvider
 * <p>
 /**
 * Database admission control: one shared resilience4j bulkhead guards all SQL executions.
 * <p>
 * An outermost transaction owns one permit for its whole lifetime (see
 * {@code BulkheadTransactionTemplate}); a statement outside a transaction owns one until its
 * connection is released.
 * <p>
 * jOOQ pairs {@code acquire()} and {@code release(connection)} per execution, but
 * {@link Bulkhead#onComplete()} is a bare {@code Semaphore.release()} with no pairing check. This
 * class therefore returns a permit only for handles it actually admitted; otherwise every
 * statement inside a transaction would inflate the semaphore permanently.
 */
public class BulkheadConnectionProvider implements ConnectionProvider {
    private record IdentityKey(Connection connection) {
        @Override
        public boolean equals(Object o) {
            return o instanceof IdentityKey(Connection connection1) && connection1 == this.connection;
        }

        @Override
        public int hashCode() {
            return System.identityHashCode(connection);
        }
    }

    private final ConnectionProvider delegate;
    private final Bulkhead bulkhead;
    private final ConcurrentMap<IdentityKey, Integer> admitted = new ConcurrentHashMap<>();

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
            if (connection != null) {
                admitted.merge(new IdentityKey(connection), 1, Integer::sum);
                handed = true;
            }
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
            if (giveBackPermit(connection)) {
                bulkhead.onComplete();
            }
        }
    }

    private boolean giveBackPermit(Connection connection) {
        IdentityKey key = new IdentityKey(connection);
        while (true) {
            Integer outstanding = admitted.get(key);
            // 从未记账：事务内的语句（事务已持格）或重复 close
            if (outstanding == null) {
                return false;
            }
            boolean updated = (outstanding == 1)
                    ? admitted.remove(key, 1)
                    : admitted.replace(key, outstanding, outstanding - 1);

            if (updated) {
                return true;
            }
        }
    }
}
