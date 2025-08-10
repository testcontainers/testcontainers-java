package org.testcontainers.containers;

import com.influxdb.v3.client.InfluxDBClient;
import com.influxdb.v3.client.Point;
import org.junit.Test;

import java.math.BigInteger;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;

public class InfluxDBContainerV3Test {

    @Test
    public void createInfluxDBContainerV3WithAuthTokenTest() {
        try (InfluxDBContainerV3<?> container = new InfluxDBContainerV3<>(InfluxDBTestUtils.INFLUXDB_V3_TEST_IMAGE)) {
            container.start();
            try (InfluxDBClient client = InfluxDBClient.getInstance(container.getUrl(), container.getToken().toCharArray(), "test")) {
                assertThat(client).isNotNull();
                assertThat(client.getServerVersion()).isEqualTo("3.3.0");
            } catch (Exception e) {
                fail("Cannot get instance of influxdb v3", e);
            }
        }
    }

    @Test
    public void createInfluxDBContainerV3WithDisableAuthTokenTest() {
        try (final InfluxDBContainerV3<?> container = new InfluxDBContainerV3<>(InfluxDBTestUtils.INFLUXDB_V3_TEST_IMAGE).withDisableAuth()) {
            container.start();
            try (InfluxDBClient client = InfluxDBClient.getInstance(container.getUrl(), null, "test")) {
                assertThat(client).isNotNull();
                assertThat(client.getServerVersion()).isEqualTo("3.3.0");
            } catch (Exception e) {
                fail("Cannot get instance of influxdb v3", e);
            }
        }
    }

    @Test
    public void tryToGetTokenWithAuthDisableTest() {
        try (final InfluxDBContainerV3<?> container = new InfluxDBContainerV3<>(InfluxDBTestUtils.INFLUXDB_V3_TEST_IMAGE).withDisableAuth()) {
            container.start();
            assertThatThrownBy(container::getToken).isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test
    public void writeAndReadResultTest() {
        try (InfluxDBContainerV3<?> container = new InfluxDBContainerV3<>(InfluxDBTestUtils.INFLUXDB_V3_TEST_IMAGE)) {
            container.start();
            try (InfluxDBClient client = InfluxDBClient.getInstance(container.getUrl(), container.getToken().toCharArray(), "test")) {
                String location = "west";
                Double value = 55.15;
                Point point = Point.measurement("temperature")
                    .setTag("location", location)
                    .setField("value", value)
                    .setTimestamp(Instant.now());
                client.writePoint(point);
                String query = "select time,location,value from temperature";
                try (Stream<Object[]> result = client.query(query)) {
                    List<Object[]> rows = result.collect(Collectors.toList());
                    assertThat(rows).hasSize(1);
                    Object[] row = rows.get(0);
                    assertThat(row[0]).isNotNull().isInstanceOf(BigInteger.class);
                    assertThat((String) row[1]).isEqualTo(location);
                    assertThat((Double) row[2]).isEqualTo(value);
                }
            } catch (Exception e) {
                fail("Cannot write or read data from influxdb v3", e);
            }
        }
    }
}
