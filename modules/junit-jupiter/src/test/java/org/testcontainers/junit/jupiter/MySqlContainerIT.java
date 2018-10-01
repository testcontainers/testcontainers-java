package org.testcontainers.junit.jupiter;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MySQLContainer;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * This test verifies, that setup and cleanup of containers works correctly.
 * It's easily achieved using the <code>MySQLContainer</code>, since it will fail
 * if the same image is running.
 *
 * @see <a href="https://github.com/testcontainers/testcontainers-spock/issues/19">Second container is started when stopping old container</a>
 */
@Testcontainers
class MySqlContainerIT {

    @Shared
    private final MySQLContainer mySQLContainer = new MySQLContainer();

    @Test
    void dummy_test() {
        assertTrue(mySQLContainer.isRunning());
    }
}
