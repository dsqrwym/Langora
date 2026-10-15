package org.dsqrwym.langora;

import org.dsqrwym.langora.infrastructure.bootstrap.SuperAdminBootstrapRunner;
import org.jooq.DSLContext;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.jooq.autoconfigure.JooqAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;

@SpringBootTest
@ActiveProfiles("test")
@EnableAutoConfiguration(exclude = {
        DataSourceAutoConfiguration.class,
        DataSourceTransactionManagerAutoConfiguration.class,
        FlywayAutoConfiguration.class,
        JooqAutoConfiguration.class
})
class LangoraApplicationTests {

    @MockitoBean
    private DSLContext dslContext;

    @MockitoBean
    private PlatformTransactionManager transactionManager;

    @MockitoBean
    private SuperAdminBootstrapRunner superAdminBootstrapRunner;

    @Test
    void contextLoads() {

    }

}
