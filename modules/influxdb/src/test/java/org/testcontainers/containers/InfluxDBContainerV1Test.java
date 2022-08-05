package org.testcontainers.containers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.hamcrest.CoreMatchers;
import org.influxdb.InfluxDB;
import org.influxdb.dto.Point;
import org.influxdb.dto.Query;
import org.influxdb.dto.QueryResult;
import org.jetbrains.annotations.Nullable;
import org.junit.After;
import org.junit.Test;

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
        try (final InfluxDBContainer influxDBContainer = new InfluxDBContainer(
            InfluxDBTestUtils.INFLUXDB_V1_TEST_IMAGE)) {

            // Start the container. This step might take some time...
            influxDBContainer.start();
            assertThat(influxDBContainer.isRunning(), CoreMatchers.is(true));

            final String actual = influxDBContainer.getUrl();

            assertThat(actual, notNullValue());
        }
    }

    @Test
    public void getNewInfluxDB() {
        try (final InfluxDBContainer influxDBContainer = new InfluxDBContainer(
            InfluxDBTestUtils.INFLUXDB_V1_TEST_IMAGE)) {

            // Start the container. This step might take some time...
            influxDBContainer.start();
            assertThat(influxDBContainer.isRunning(), CoreMatchers.is(true));

            final InfluxDB influxDBClient = influxDBContainer.getNewInfluxDB();

            assertThat(influxDBClient, notNullValue());
            assertThat(influxDBClient.ping(), notNullValue());
        }
    }

    @Test
    public void getLivenessCheckPort() {
        try (final InfluxDBContainer influxDBContainer = new InfluxDBContainer(
            InfluxDBTestUtils.INFLUXDB_V1_TEST_IMAGE)) {

            // Start the container. This step might take some time...
            influxDBContainer.start();
            assertThat(influxDBContainer.isRunning(), CoreMatchers.is(true));

            final Set<Integer> actual = influxDBContainer.getLivenessCheckPortNumbers();

            assertThat(actual, notNullValue());
        }
    }

    @Test
    public void describeDatabases() {
        try (final InfluxDBContainer influxDBContainer = new InfluxDBContainer(
            InfluxDBTestUtils.INFLUXDB_V1_TEST_IMAGE)) {
            influxDBContainer.withDatabase(DATABASE)
                .withUsername(USER)
                .withPassword(PASSWORD);

            // Start the container. This step might take some time...
            influxDBContainer.start();
            assertThat(influxDBContainer.isRunning(), CoreMatchers.is(true));

            this.influxDBClient = influxDBContainer.getNewInfluxDB();

            assertThat(this.influxDBClient, notNullValue());
            assertThat(this.influxDBClient.describeDatabases(), hasItem(DATABASE));
        }
    }

    @Test
    public void checkVersion() {
        try (final InfluxDBContainer influxDBContainer = new InfluxDBContainer(
            InfluxDBTestUtils.INFLUXDB_V1_TEST_IMAGE)) {
            influxDBContainer.withDatabase(DATABASE)
                .withUsername(USER)
                .withPassword(PASSWORD);

            // Start the container. This step might take some time...
            influxDBContainer.start();
            assertThat(influxDBContainer.isRunning(), CoreMatchers.is(true));

            this.influxDBClient = influxDBContainer.getNewInfluxDB();

            assertThat(this.influxDBClient, notNullValue());

            assertThat(this.influxDBClient.ping(), notNullValue());

            assertThat(this.influxDBClient.version(), is(TEST_VERSION));
        }
    }

    @Test
    public void queryForWriteAndRead() {
        try (final InfluxDBContainer influxDBContainer = new InfluxDBContainer(
            InfluxDBTestUtils.INFLUXDB_V1_TEST_IMAGE)) {
            influxDBContainer.withDatabase(DATABASE)
                .withUsername(USER)
                .withPassword(PASSWORD);

            // Start the container. This step might take some time...
            influxDBContainer.start();
            assertThat(influxDBContainer.isRunning(), CoreMatchers.is(true));

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

            assertThat(actual, notNullValue());
            assertThat(actual.getError(), nullValue());
            assertThat(actual.getResults(), notNullValue());
            assertThat(actual.getResults().size(), is(1));
        }
    }
}
