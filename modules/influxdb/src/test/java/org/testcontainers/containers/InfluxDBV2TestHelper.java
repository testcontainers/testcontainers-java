package org.testcontainers.containers;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.client.InfluxDBClientOptions;

public final class InfluxDBV2TestHelper {

    private InfluxDBV2TestHelper() {
    }

    public static InfluxDBClient getInfluxDBClient(final InfluxDBContainer influxDBContainer) {
        final InfluxDBClientOptions influxDBClientOptions = InfluxDBClientOptions.builder()
            .url(influxDBContainer.getUrl())
            .authenticate(influxDBContainer.getUsername(), influxDBContainer.getPassword().toCharArray())
            .bucket(influxDBContainer.getBucket())
            .org(influxDBContainer.getOrganization())
            .build();
        return InfluxDBClientFactory.create(influxDBClientOptions);
    }
}
