package org.testcontainers.containers;

import org.influxdb.InfluxDB;
import org.influxdb.dto.Point;
import org.influxdb.dto.Query;
import org.influxdb.dto.QueryResult;
import org.jetbrains.annotations.Nullable;
import org.junit.After;
import org.junit.Test;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

public class InfluxDBContainerV1Test {

    private static final String TEST_VERSION = InfluxDBTestUtils.INFLUXDB_V1_TEST_IMAGE.getVersionPart();

    private static final String DATABASE = "test";

    private static final String USER = "new-test-user";

    private static final String PASSWORD = "new-test-password";

    @Nullable
    private InfluxDB influxDBClient = null;

    @After
    public void stopInfluxDBClient() {
        if (this.influxDBClient != null) {
            this.influxDBClient.close();
            this.influxDBClient = null;
        }
    }

    @Test
    public void getUrl() {
        try (
            final InfluxDBContainer influxDBContainer = new InfluxDBContainer(InfluxDBTestUtils.INFLUXDB_V1_TEST_IMAGE)
        ) {
            // Start the container. This step might take some time...
            influxDBContainer.start();
            assertThat(influxDBContainer.isRunning()).isTrue();

            final String actual = influxDBContainer.getUrl();

            assertThat(actual).isNotNull();
        }
    }

    @Test
    public void getNewInfluxDB() {
        try (
            final InfluxDBContainer influxDBContainer = new InfluxDBContainer(InfluxDBTestUtils.INFLUXDB_V1_TEST_IMAGE)
        ) {
            // Start the container. This step might take some time...
            influxDBContainer.start();
            assertThat(influxDBContainer.isRunning()).isTrue();

            final InfluxDB influxDBClient = influxDBContainer.getNewInfluxDB();

            assertThat(influxDBClient).isNotNull();
            assertThat(influxDBClient.ping().isGood()).isTrue();
        }
    }

    @Test
    public void getLivenessCheckPort() {
        try (
            final InfluxDBContainer influxDBContainer = new InfluxDBContainer(InfluxDBTestUtils.INFLUXDB_V1_TEST_IMAGE)
        ) {
            // Start the container. This step might take some time...
            influxDBContainer.start();
            assertThat(influxDBContainer.isRunning()).isTrue();

            final Set<Integer> actual = influxDBContainer.getLivenessCheckPortNumbers();

            assertThat(actual).isNotNull();
        }
    }

    @Test
    public void describeDatabases() {
        try (
            final InfluxDBContainer influxDBContainer = new InfluxDBContainer(InfluxDBTestUtils.INFLUXDB_V1_TEST_IMAGE)
        ) {
            influxDBContainer.withDatabase(DATABASE).withUsername(USER).withPassword(PASSWORD);

            // Start the container. This step might take some time...
            influxDBContainer.start();
            assertThat(influxDBContainer.isRunning()).isTrue();

            this.influxDBClient = influxDBContainer.getNewInfluxDB();

            assertThat(this.influxDBClient).isNotNull();
            assertThat(this.influxDBClient.describeDatabases()).contains(DATABASE);
        }
    }

    @Test
    public void checkVersion() {
        try (
            final InfluxDBContainer influxDBContainer = new InfluxDBContainer(InfluxDBTestUtils.INFLUXDB_V1_TEST_IMAGE)
        ) {
            influxDBContainer.withDatabase(DATABASE).withUsername(USER).withPassword(PASSWORD);

            // Start the container. This step might take some time...
            influxDBContainer.start();
            assertThat(influxDBContainer.isRunning()).isTrue();

            this.influxDBClient = influxDBContainer.getNewInfluxDB();

            assertThat(this.influxDBClient).isNotNull();
            assertThat(this.influxDBClient.ping().isGood()).isTrue();
            assertThat(this.influxDBClient.version()).isEqualTo(TEST_VERSION);
        }
    }

    @Test
    public void queryForWriteAndRead() {
        try (
            final InfluxDBContainer influxDBContainer = new InfluxDBContainer(InfluxDBTestUtils.INFLUXDB_V1_TEST_IMAGE)
        ) {
            influxDBContainer.withDatabase(DATABASE).withUsername(USER).withPassword(PASSWORD);

            // Start the container. This step might take some time...
            influxDBContainer.start();
            assertThat(influxDBContainer.isRunning()).isTrue();

            this.influxDBClient = influxDBContainer.getNewInfluxDB();

            final Point point = Point
                .measurement("cpu")
                .time(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
                .addField("idle", 90L)
                .addField("user", 9L)
                .addField("system", 1L)
                .build();
            this.influxDBClient.write(point);

            final Query query = new Query("SELECT idle FROM cpu", DATABASE);
            final QueryResult actual = this.influxDBClient.query(query);

            assertThat(actual).isNotNull();
            assertThat(actual.getError()).isNull();
            assertThat(actual.getResults()).isNotNull();
            assertThat(actual.getResults()).hasSize(1);
        }
    }
}
