package org.testcontainers.spock

import org.testcontainers.containers.InfluxDBContainer
import spock.lang.Shared
import spock.lang.Specification

import static org.testcontainers.containers.InfluxDBContainer.VERSION

@Testcontainers
class InfluxDBContainerIT extends Specification {

    private static final String DATABASE = "terst"
    private static final String USER = "testuser"
    private static final String PASSWORD = "testpassword"

    @Shared
    public InfluxDBContainer influxDBContainer = new InfluxDBContainer(VERSION)
        .withDatabase(DATABASE)
        .withUsername(USER)
        .withPassword(PASSWORD)

    def "dummy test"() {
        expect:
        influxDBContainer.isRunning()
        influxDBContainer.url
        influxDBContainer.newInfluxDB
        influxDBContainer.newInfluxDB.ping()
    }
}
