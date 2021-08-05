package org.testcontainers.containers;

import org.influxdb.InfluxDB;
import org.influxdb.dto.Point;
import org.influxdb.dto.Query;
import org.influxdb.dto.QueryResult;
import org.junit.Rule;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

public class InfluxDBContainerV1WithUserTest {

    private static final String TEST_VERSION = InfluxDBTestImages.INFLUXDB_V1_TEST_IMAGE.getVersionPart();
    private static final String DATABASE = "test";
    private static final String USER = "test-user";
    private static final String PASSWORD = "test-password";

    @Rule
    public InfluxDBContainerV1 influxDBContainer = new InfluxDBContainer(InfluxDBTestImages.INFLUXDB_V1_TEST_IMAGE)
        .withDatabase(DATABASE)
        .withUsername(USER)
        .withPassword(PASSWORD);

    @Test
    public void describeDatabases() {
        final InfluxDB actual = this.influxDBContainer.getNewInfluxDB();

        assertThat(actual, notNullValue());
        assertThat(actual.describeDatabases(), hasItem(DATABASE));
    }

    @Test
    public void checkVersion() {
        final InfluxDB actual = this.influxDBContainer.getNewInfluxDB();

        assertThat(actual, notNullValue());

        assertThat(actual.ping(), notNullValue());
        assertThat(actual.ping().getVersion(), is(TEST_VERSION));

        assertThat(actual.version(), is(TEST_VERSION));
    }

    @Test
    public void queryForWriteAndRead() {
        final InfluxDB influxDB = this.influxDBContainer.getNewInfluxDB();

        final Point point = Point.measurement("cpu")
            .time(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
            .addField("idle", 90L)
            .addField("user", 9L)
            .addField("system", 1L)
            .build();
        influxDB.write(point);

        final Query query = new Query("SELECT idle FROM cpu", DATABASE);
        final QueryResult actual = influxDB.query(query);

        assertThat(actual, notNullValue());
        assertThat(actual.getError(), nullValue());
        assertThat(actual.getResults(), notNullValue());
        assertThat(actual.getResults().size(), is(1));

    }
}
