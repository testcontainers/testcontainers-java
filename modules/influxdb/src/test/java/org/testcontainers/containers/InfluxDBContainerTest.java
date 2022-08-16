package org.testcontainers.containers;

import org.influxdb.InfluxDB;
import org.junit.ClassRule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class InfluxDBContainerTest {

    @ClassRule
    public static InfluxDBContainer<?> influxDBContainer = new InfluxDBContainer<>(
        InfluxDBTestImages.INFLUXDB_TEST_IMAGE
    );

    @Test
    public void getUrl() {
        String actual = influxDBContainer.getUrl();

        assertThat(actual).isNotNull();
    }

    @Test
    public void getNewInfluxDB() {
        InfluxDB actual = influxDBContainer.getNewInfluxDB();

        assertThat(actual).isNotNull();
        assertThat(actual.ping()).isNotNull();
    }

    @Test
    public void getLivenessCheckPort() {
        Integer actual = influxDBContainer.getLivenessCheckPort();

        assertThat(actual).isNotNull();
    }

    @Test
    public void isRunning() {
        boolean actual = influxDBContainer.isRunning();

        assertThat(actual).isTrue();
    }
}
