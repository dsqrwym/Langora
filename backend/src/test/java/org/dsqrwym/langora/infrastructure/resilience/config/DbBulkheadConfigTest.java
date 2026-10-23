package org.dsqrwym.langora.infrastructure.resilience.config;

import com.zaxxer.hikari.HikariDataSource;
import io.github.resilience4j.bulkhead.Bulkhead;
import org.dsqrwym.langora.infrastructure.resilience.DbBulkheadProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * The {@code enabled} switch is expressed by bean presence, so these tests assert on the context
 * rather than on an internal flag.
 */
class DbBulkheadConfigTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(DbBulkheadConfig.class)
            .withBean(DbBulkheadProperties.class, () -> properties(0))
            .withBean(DataSource.class, () -> mock(DataSource.class));

    private static DbBulkheadProperties properties(int maxConcurrentCalls) {
        return new DbBulkheadProperties(true, maxConcurrentCalls, Duration.ofSeconds(2), true);
    }

    @Test
    void createsTheBulkheadByDefault() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(Bulkhead.class);
        });
    }

    @Test
    void createsNoBulkheadWhenDisabled() {
        runner.withPropertyValues("langora.resilience.db.bulkhead.enabled=false")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean(Bulkhead.class);
                });
    }

    @Test
    void explicitPermitCountWinsOverThePool() {
        HikariDataSource hikari = mock(HikariDataSource.class);
        given(hikari.getMaximumPoolSize()).willReturn(20);

        assertThat(DbBulkheadConfig.resolveMaxConcurrentCalls(properties(7), hikari)).isEqualTo(7);
    }

    @Test
    void derivesThePermitCountFromTheHikariPool() {
        HikariDataSource hikari = mock(HikariDataSource.class);
        given(hikari.getMaximumPoolSize()).willReturn(20);

        assertThat(DbBulkheadConfig.resolveMaxConcurrentCalls(properties(0), hikari)).isEqualTo(20);
    }

    @Test
    void fallsBackToAPermitCountThatNeverExceedsTheDefaultPool() {
        assertThat(DbBulkheadConfig.resolveMaxConcurrentCalls(properties(0), null)).isEqualTo(20);
        assertThat(DbBulkheadConfig.resolveMaxConcurrentCalls(properties(0), mock(DataSource.class))).isEqualTo(20);
    }

    @Test
    void bindsTheDocumentedDefaults() {
        new ApplicationContextRunner()
                .withUserConfiguration(BindingConfiguration.class)
                .run(context -> {
                    assertThat(context).hasNotFailed();

                    DbBulkheadProperties properties = context.getBean(DbBulkheadProperties.class);
                    assertThat(properties.enabled()).isTrue();
                    assertThat(properties.maxConcurrentCalls()).isZero();
                    assertThat(properties.maxWaitDuration()).isEqualTo(Duration.ofSeconds(2));
                    assertThat(properties.fairCallHandlingEnabled()).isTrue();
                });
    }

    @Test
    void rejectsANegativePermitCountAtStartup() {
        new ApplicationContextRunner()
                .withUserConfiguration(BindingConfiguration.class)
                .withPropertyValues("langora.resilience.db.bulkhead.max-concurrent-calls=-1")
                .run(context -> assertThat(context).hasFailed());
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(DbBulkheadProperties.class)
    static class BindingConfiguration {
    }
}
