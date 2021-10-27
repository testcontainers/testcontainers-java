package org.testcontainers.containers;

import org.influxdb.InfluxDB;
import org.junit.ClassRule;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

public class InfluxDBContainerTest {

    @ClassRule
    public static InfluxDBContainer<?> influxDBContainer = new InfluxDBContainer<>(InfluxDBTestImages.INFLUXDB_TEST_IMAGE);

    @Test
    public void getUrl() {
        String actual = influxDBContainer.getUrl();

        assertThat(actual, notNullValue());
    }

    @Test
    public void getNewInfluxDB() {
        InfluxDB actual = influxDBContainer.getNewInfluxDB();

        assertThat(actual, notNullValue());
        assertThat(actual.ping(), notNullValue());
    }

    @Test
    public void getLivenessCheckPort() {
        Integer actual = influxDBContainer.getLivenessCheckPort();

        assertThat(actual, notNullValue());
    }

    @Test
    public void isRunning() {
        boolean actual = influxDBContainer.isRunning();

        assertThat(actual, is(true));
    }
}
