package org.dsqrwym.langora.infrastructure.transaction;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import org.dsqrwym.langora.infrastructure.resilience.config.DbBulkheadConfig;
import org.dsqrwym.langora.infrastructure.resilience.tx.BulkheadTransactionTemplate;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * {@code enabled=false} must remove the permit wrapper from the call chain, not merely deactivate it.
 */
class TransactionConfigTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(TransactionConfig.class)
            .withBean(PlatformTransactionManager.class, () -> mock(PlatformTransactionManager.class));

    @Test
    void wrapsTheTransactionBoundaryWhenTheBulkheadIsPresent() {
        runner.withBean(DbBulkheadConfig.DB_BULKHEAD_NAME, Bulkhead.class,
                        () -> Bulkhead.of(DbBulkheadConfig.DB_BULKHEAD_NAME,
                                BulkheadConfig.custom().maxConcurrentCalls(1).build()))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(TransactionTemplate.class))
                            .isInstanceOf(BulkheadTransactionTemplate.class);
                });
    }

    @Test
    void staysAPlainTemplateWhenTheBulkheadIsAbsent() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBean(TransactionTemplate.class))
                    .isNotInstanceOf(BulkheadTransactionTemplate.class);
        });
    }
}
