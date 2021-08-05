package org.testcontainers.containers;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.client.InfluxDBClientOptions;

public final class InfluxDBV2TestHelper {

    private InfluxDBV2TestHelper() {
    }

    public static InfluxDBClient getInfluxDBClient(final InfluxDBContainerV2 influxDBContainerV2) {
        final InfluxDBClientOptions influxDBClientOptions = InfluxDBClientOptions.builder()
            .url(influxDBContainerV2.getUrl())
            .authenticate(influxDBContainerV2.getUsername(), influxDBContainerV2.getPassword().toCharArray())
            .bucket(influxDBContainerV2.getBucket())
            .org(influxDBContainerV2.getOrganization())
            .build();
        return InfluxDBClientFactory.create(influxDBClientOptions);
    }
}
