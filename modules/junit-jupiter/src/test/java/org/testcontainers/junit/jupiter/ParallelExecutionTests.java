package org.testcontainers.junit.jupiter;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.PostgreSQLContainer;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(parallel = true)
public class ParallelExecutionTests {

    @Container
    private static final PostgreSQLContainer<?> POSTGRESQL_CONTAINER = new PostgreSQLContainer<>(
        JUnitJupiterTestImages.POSTGRES_IMAGE
    )
        .withDatabaseName("foo")
        .withUsername("foo")
        .withPassword("secret");

    @Container
    private MySQLContainer<?> mySQLContainer = new MySQLContainer<>(JUnitJupiterTestImages.MYSQL_IMAGE);

    @Test
    void test() {
        assertThat(POSTGRESQL_CONTAINER.isRunning()).isTrue();
        assertThat(mySQLContainer.isRunning()).isTrue();
    }
}
