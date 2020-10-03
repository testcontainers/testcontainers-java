package org.testcontainers.junit.jqwik;

import net.jqwik.api.Property;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.PostgreSQLContainer;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
class MixedLifecycleTests {

    // will be shared between properties
    @Container
    private static final MySQLContainer MY_SQL_CONTAINER = new MySQLContainer();

    // will be started before and stopped after each property
    @Container
    private PostgreSQLContainer postgresqlContainer = new PostgreSQLContainer()
        .withDatabaseName("foo")
        .withUsername("foo")
        .withPassword("secret");

    @Property
    void test() {
        assertThat(MY_SQL_CONTAINER.isRunning()).isTrue();
        assertThat(postgresqlContainer.isRunning()).isTrue();
    }
}
