package org.testcontainers.junit.jqwik;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.PostgreSQLContainer;

import static org.junit.jupiter.api.Assertions.assertTrue;


// testClass {
@Testcontainers
class MixedLifecycleTests {

    // will be shared between test methods
    @TestContainer
    private static final MySQLContainer MY_SQL_CONTAINER = new MySQLContainer();

    // will be started before and stopped after each test method
    @TestContainer
    private PostgreSQLContainer postgresqlContainer = new PostgreSQLContainer()
        .withDatabaseName("foo")
        .withUsername("foo")
        .withPassword("secret");

    @Test
    void test() {
        assertTrue(MY_SQL_CONTAINER.isRunning());
        assertTrue(postgresqlContainer.isRunning());
    }
}
// }
