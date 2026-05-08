package com.terry.duey;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Import(ApiTestConfig.class)
class EnvironmentConfigTest {
    @Autowired Environment environment;

    @Autowired DataSource dataSource;

    @Test
    void testProfileUsesInMemorySqliteWithoutStageOrProdSecrets() throws Exception {
        assertThat(environment.getActiveProfiles()).contains("test");
        try (Connection connection = dataSource.getConnection()) {
            assertThat(connection.getMetaData().getURL()).contains("mode=memory");
        }
        assertThat(environment.getProperty("duey.auth.google-client-id"))
                .isEqualTo("test-google-client");
    }
}
