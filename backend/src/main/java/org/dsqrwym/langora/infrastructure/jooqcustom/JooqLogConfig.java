package org.dsqrwym.langora.infrastructure.jooqcustom;

import org.jooq.impl.DefaultExecuteListener;
import org.jooq.impl.DefaultExecuteListenerProvider;
import org.springframework.boot.jooq.autoconfigure.DefaultConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JooqLogConfig {
    @Bean
    public DefaultConfigurationCustomizer defaultConfigurationCustomizer() {
        return (configuration) -> configuration.set(new DefaultExecuteListenerProvider(new SqlExecutionLoggerListener()));
    }
}
