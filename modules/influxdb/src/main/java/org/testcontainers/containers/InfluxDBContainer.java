package org.testcontainers.containers;

import org.testcontainers.utility.DockerImageName;

/**
 * @deprecated instead use {@link InfluxDBContainerV1} for InfluxDB 1.x or {@link InfluxDBContainerV2} for InfluxDB 2.x instead
 */
@Deprecated
public class InfluxDBContainer extends InfluxDBContainerV1 {
    private static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse("influxdb");
    private static final String DEFAULT_TAG = "1.4.3";

    /**
     * @deprecated use {@link InfluxDBContainer(DockerImageName)} instead
     */
    @Deprecated
    public InfluxDBContainer() {
        this(DEFAULT_IMAGE_NAME.withTag(DEFAULT_TAG));
    }

    /**
     * @deprecated use {@link InfluxDBContainer(DockerImageName)} instead
     */
    @Deprecated
    public InfluxDBContainer(final String version) {
        this(DEFAULT_IMAGE_NAME.withTag(version));
    }

    public InfluxDBContainer(final DockerImageName influxdbTestImage) {
        super(influxdbTestImage);
    }
}
