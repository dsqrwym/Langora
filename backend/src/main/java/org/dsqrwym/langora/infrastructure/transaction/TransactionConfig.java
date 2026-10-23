package org.dsqrwym.langora.infrastructure.transaction;

import io.github.resilience4j.bulkhead.Bulkhead;
import org.dsqrwym.langora.infrastructure.resilience.config.DbBulkheadConfig;
import org.dsqrwym.langora.infrastructure.resilience.tx.BulkheadTransactionTemplate;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Configuration
public class TransactionConfig {
    @Bean
    public TransactionTemplate transactionTemplate(
            PlatformTransactionManager transactionManager,
            @Qualifier(DbBulkheadConfig.DB_BULKHEAD_NAME) ObjectProvider<Bulkhead> dbBulkheadProvider
    ) {
        Bulkhead dbBulkhead = dbBulkheadProvider.getIfAvailable();
        if (dbBulkhead != null) {
            return new BulkheadTransactionTemplate(transactionManager, dbBulkhead);
        }
        return new TransactionTemplate(transactionManager);
    }
}
