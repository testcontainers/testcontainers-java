package org.testcontainers.containers;

import org.influxdb.InfluxDB;
import org.junit.ClassRule;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

public class InfluxDBContainerV1Test {

    @ClassRule
    public static InfluxDBContainer<?> influxDBContainer = new InfluxDBContainer<>(
        InfluxDBTestImages.INFLUXDB_V1_TEST_IMAGE
    );

    @Test
    public void getUrl() {
        final String actual = influxDBContainer.getUrl();

        assertThat(actual, notNullValue());
    }

    @Test
    public void getNewInfluxDB() {
        final InfluxDB actual = influxDBContainer.getNewInfluxDB();

        assertThat(actual, notNullValue());
        assertThat(actual.ping(), notNullValue());
    }

    @Test
    public void getLivenessCheckPort() {
        final Integer actual = influxDBContainer.getLivenessCheckPort();

        assertThat(actual, notNullValue());
    }

    @Test
    public void isRunning() {
        final boolean actual = influxDBContainer.isRunning();

        assertThat(actual, is(true));
    }
}
