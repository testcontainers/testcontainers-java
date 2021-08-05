package org.testcontainers.containers;

import org.testcontainers.utility.DockerImageName;

/**
 * @deprecated instead use {@link InfluxDBContainerV1#InfluxDBContainerV1(DockerImageName) } for InfluxDB 1.x or {@link
 * InfluxDBContainerV2#InfluxDBContainerV2(DockerImageName)} for InfluxDB 2.x instead
 */
@Deprecated
public class InfluxDBContainer extends InfluxDBContainerV1 {
    public InfluxDBContainer() {
        super();
    }

    public InfluxDBContainer(final String version) {
        super(version);
    }

    public InfluxDBContainer(final DockerImageName influxdbTestImage) {
        super(influxdbTestImage);
    }
}
