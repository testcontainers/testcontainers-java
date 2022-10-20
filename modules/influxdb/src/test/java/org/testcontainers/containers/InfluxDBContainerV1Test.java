package org.testcontainers.containers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testcontainers.containers.InfluxDBTestUtils.createInfluxDBWithUrl;

import java.util.concurrent.TimeUnit;
import org.influxdb.InfluxDB;
import org.influxdb.dto.Point;
import org.influxdb.dto.Query;
import org.influxdb.dto.QueryResult;
import org.junit.Test;
import org.testcontainers.utility.DockerImageName;

public class InfluxDBContainerV1Test {

    private static final String TEST_VERSION = InfluxDBTestUtils.INFLUXDB_V1_TEST_IMAGE.getVersionPart();

    private static final String DATABASE = "test";

    private static final String USER = "new-test-user";

    private static final String PASSWORD = "new-test-password";

    @Test
    public void createInfluxDBOnlyWithUrlAndCorrectVersion() {
        try (
            // constructorWithDefaultVariables {
            final InfluxDBContainer<?> influxDBContainer = new InfluxDBContainer<>(
                DockerImageName.parse("influxdb:1.4.3"))
            // }
        ) {
            // Start the container. This step might take some time...
            influxDBContainer.start();

            final String actual = influxDBContainer.getUrl();

            try (final InfluxDB influxDBClient = createInfluxDBWithUrl(actual)) {
                assertThat(influxDBClient).isNotNull();
                assertThat(influxDBClient.ping().isGood()).isTrue();
                assertThat(influxDBClient.version()).isEqualTo(TEST_VERSION);
            }
        }
    }

    @Test
    public void getNewInfluxDBWithCorrectVersion() {
        try (
            final InfluxDBContainer<?> influxDBContainer = new InfluxDBContainer<>(
                InfluxDBTestUtils.INFLUXDB_V1_TEST_IMAGE)
        ) {
            // Start the container. This step might take some time...
            influxDBContainer.start();

            try (final InfluxDB influxDBClient = influxDBContainer.getNewInfluxDB()) {
                assertThat(influxDBClient).isNotNull();
                assertThat(influxDBClient.ping().isGood()).isTrue();
                assertThat(influxDBClient.version()).isEqualTo(TEST_VERSION);
            }
        }
    }

    @Test
    public void describeDatabases() {
        try (
            // constructorWithUserPassword {
            final InfluxDBContainer<?> influxDBContainer = new InfluxDBContainer<>(
                DockerImageName.parse("influxdb:1.4.3"))
                .withDatabase(DATABASE)
                .withUsername(USER)
                .withPassword(PASSWORD)
            // }
        ) {
            // Start the container. This step might take some time...
            influxDBContainer.start();

            try (final InfluxDB influxDBClient = influxDBContainer.getNewInfluxDB()) {
                assertThat(influxDBClient.describeDatabases()).contains(DATABASE);
            }
        }
    }

    @Test
    public void queryForWriteAndRead() {
        try (
            final InfluxDBContainer<?> influxDBContainer = new InfluxDBContainer<>(
                InfluxDBTestUtils.INFLUXDB_V1_TEST_IMAGE)
                .withDatabase(DATABASE)
                .withUsername(USER)
                .withPassword(PASSWORD)
        ) {
            // Start the container. This step might take some time...
            influxDBContainer.start();

            try (final InfluxDB influxDBClient = influxDBContainer.getNewInfluxDB()) {
                final Point point = Point
                    .measurement("cpu")
                    .time(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
                    .addField("idle", 90L)
                    .addField("user", 9L)
                    .addField("system", 1L)
                    .build();
                influxDBClient.write(point);

                final Query query = new Query("SELECT idle FROM cpu", DATABASE);
                final QueryResult actual = influxDBClient.query(query);

                assertThat(actual).isNotNull();
                assertThat(actual.getError()).isNull();
                assertThat(actual.getResults()).isNotNull();
                assertThat(actual.getResults()).hasSize(1);
            }
        }
    }
}
