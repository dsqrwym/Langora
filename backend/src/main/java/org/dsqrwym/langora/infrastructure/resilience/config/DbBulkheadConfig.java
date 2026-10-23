package org.dsqrwym.langora.infrastructure.resilience.config;

import com.zaxxer.hikari.HikariDataSource;
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import lombok.extern.slf4j.Slf4j;
import org.dsqrwym.langora.infrastructure.resilience.DbBulkheadProperties;
import org.dsqrwym.langora.infrastructure.resilience.jooq.BulkheadConnectionProvider;
import org.jooq.ConnectionProvider;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.jooq.autoconfigure.DefaultConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Slf4j
@Configuration(proxyBeanMethods = false)
public class DbBulkheadConfig {
    public static final String DB_BULKHEAD_NAME = "dbBulkhead";

    private static final int FALLBACK_MAX_CONCURRENT_CALLS = 20;

    @Bean
    @ConditionalOnProperty(prefix = DbBulkheadProperties.PREFIX, name = "enabled", havingValue = "true", matchIfMissing = true)
    public Bulkhead dbBulkhead(DbBulkheadProperties properties, ObjectProvider<DataSource> dataSourceProvider) {
        final DataSource dataSource = dataSourceProvider.getIfAvailable();
        final int permits = resolveMaxConcurrentCalls(properties, dataSource);
        if (dataSource instanceof HikariDataSource hikari && permits > hikari.getMaximumPoolSize()) {
            log.warn("DB bulkhead permits ({}) exceed the Hikari pool size ({}); rejected requests will queue for a connection "
                    + "instead of failing fast", permits, hikari.getMaximumPoolSize());
        }

        Bulkhead bulkhead = Bulkhead.of(
                DB_BULKHEAD_NAME,
                BulkheadConfig.custom()
                        .maxConcurrentCalls(permits)
                        .maxWaitDuration(properties.maxWaitDuration())
                        .fairCallHandlingStrategyEnabled(properties.fairCallHandlingEnabled())
                        .build()
        );
        log.info("DB bulkhead '{}' permits={} maxWait={} fair={}",
                DB_BULKHEAD_NAME, permits, properties.maxWaitDuration(), properties.fairCallHandlingEnabled());

        return bulkhead;
    }

    @Bean
    @ConditionalOnProperty(prefix = DbBulkheadProperties.PREFIX, name = "enabled", havingValue = "true", matchIfMissing = true)
    public DefaultConfigurationCustomizer dbBulkheadConfigCustomizer(Bulkhead dbBulkhead) {
        return configuration -> {
            ConnectionProvider delegate = configuration.connectionProvider();
            if (delegate instanceof BulkheadConnectionProvider) {
                return;
            }

            configuration.set(new BulkheadConnectionProvider(delegate, dbBulkhead));
            log.info("DB bulkhead installed on top of {}", delegate.getClass().getName());
        };
    }

    public static int resolveMaxConcurrentCalls(DbBulkheadProperties properties, @Nullable DataSource dataSource) {
        if (properties.maxConcurrentCalls() > 0) {
            return properties.maxConcurrentCalls();
        }
        if (dataSource instanceof HikariDataSource hikari) {
            return hikari.getMaximumPoolSize();
        }

        log.warn("Could not resolve max concurrent calls for bulkhead. Using default value of {}", FALLBACK_MAX_CONCURRENT_CALLS);
        return FALLBACK_MAX_CONCURRENT_CALLS;
    }
}
