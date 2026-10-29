package org.dsqrwym.langora.infrastructure.resilience.tx;

import io.github.resilience4j.bulkhead.Bulkhead;
import org.dsqrwym.langora.infrastructure.web.exception.RateLimitedException;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Acquires the database permit before the transaction begins, i.e. before
 * {@code DataSourceTransactionManager.doBegin()} pulls a connection out of the pool.
 * <p>
 * With permits == pool size, {@code getConnection()} never queues, so an overloaded request is
 * rejected within {@code maxWaitDuration} instead of waiting for Hikari's connection timeout.
 */
public class BulkheadTransactionTemplate extends TransactionTemplate {
    private final Bulkhead bulkhead;

    public BulkheadTransactionTemplate(PlatformTransactionManager transactionManager, Bulkhead bulkhead) {
        super(transactionManager);
        this.bulkhead = bulkhead;
    }

    @Override
    public <T extends @Nullable Object> T execute(@NonNull TransactionCallback<T> action) throws TransactionException {
        // Joining an outer transaction (PROPAGATION_REQUIRED): it already owns the permit.
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            return super.execute(action);
        }

        if (!bulkhead.tryAcquirePermission()) {
            throw new RateLimitedException(bulkhead.getBulkheadConfig().getMaxWaitDuration());
        }

        try {
            return super.execute(action);
        } finally {
            bulkhead.onComplete();
        }
    }
}
