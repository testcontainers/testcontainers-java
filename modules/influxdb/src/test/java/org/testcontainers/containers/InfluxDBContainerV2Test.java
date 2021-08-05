package org.testcontainers.containers;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.domain.HealthCheck.StatusEnum;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

public class InfluxDBContainerV2Test {

    private static final String TEST_VERSION = InfluxDBTestImages.INFLUXDB_V2_TEST_IMAGE.getVersionPart();

    @ClassRule
    public static final InfluxDBContainerV2 influxDBContainerV2 =
        new InfluxDBContainerV2(InfluxDBTestImages.INFLUXDB_V2_TEST_IMAGE);

    private InfluxDBClient client = null;

    @Before
    public void setUp() {
        this.client = influxDBContainerV2.getInfluxDBClient();
    }

    @After
    public void tearDown() {
        this.client.close();
    }

    @Test
    public void getUrl() {
        final String actual = influxDBContainerV2.getUrl();

        assertThat(actual, notNullValue());
    }

    @Test
    public void getNewInfluxDB() {
        final InfluxDBClient actual = influxDBContainerV2.getInfluxDBClient();

        assertThat(actual, notNullValue());
        assertThat(actual.health().getStatus(), is(StatusEnum.PASS));
    }

    @Test
    public void checkVersion() {
        assertThat(this.client, notNullValue());

        assertThat(this.client.health().getStatus(), is(StatusEnum.PASS));

        final String actualVersion = String.format("%s", this.client.health().getVersion());

        assertThat(actualVersion, is(TEST_VERSION));
    }

    @Test
    public void isRunning() {
        final boolean actual = influxDBContainerV2.isRunning();

        assertThat(actual, is(true));
    }
}
