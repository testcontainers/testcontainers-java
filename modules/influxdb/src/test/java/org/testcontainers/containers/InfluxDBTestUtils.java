package org.testcontainers.containers;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.client.InfluxDBClientOptions;
import org.testcontainers.utility.DockerImageName;

public final class InfluxDBTestUtils {

    static final DockerImageName INFLUXDB_V1_TEST_IMAGE = DockerImageName.parse("influxdb:1.4.3");

    static final DockerImageName INFLUXDB_V2_TEST_IMAGE = DockerImageName.parse("influxdb:2.0.7");

    private InfluxDBTestUtils() {}

    public static InfluxDBClient getInfluxDBClient(final InfluxDBContainer influxDBContainer) {
        final InfluxDBClientOptions influxDBClientOptions = InfluxDBClientOptions
            .builder()
            .url(influxDBContainer.getUrl())
            .authenticate(influxDBContainer.getUsername(), influxDBContainer.getPassword().toCharArray())
            .bucket(influxDBContainer.getBucket())
            .org(influxDBContainer.getOrganization())
            .build();
        return InfluxDBClientFactory.create(influxDBClientOptions);
    }
}
