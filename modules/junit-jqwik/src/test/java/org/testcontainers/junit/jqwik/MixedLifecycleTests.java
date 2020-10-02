package org.testcontainers.junit.jqwik;

import net.jqwik.api.Property;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.PostgreSQLContainer;

import static org.junit.jupiter.api.Assertions.assertTrue;

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
        assertTrue(MY_SQL_CONTAINER.isRunning());
        assertTrue(postgresqlContainer.isRunning());
    }
}
