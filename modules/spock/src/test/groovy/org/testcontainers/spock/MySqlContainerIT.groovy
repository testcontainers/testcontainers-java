package org.testcontainers.spock

import org.testcontainers.containers.MySQLContainer
import spock.lang.Shared
import spock.lang.Specification

/**
 * This test verifies, that setup and cleanup of containers works correctly.
 * It's easily achieved using the <code>MySQLContainer</code>, since it will fail
 * if the same image is running.
 *
 * @see <a href="https://github.com/testcontainers/testcontainers-spock/issues/19">Second container is started when stopping old container</a>
 */
@Testcontainers
class MySqlContainerIT extends Specification {

    @Shared
    MySQLContainer mySQLContainer = new MySQLContainer(SpockTestImages.MYSQL_IMAGE)

    def "dummy test"() {
        expect:
        mySQLContainer.isRunning()
    }


}
