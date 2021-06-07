package org.testcontainers.containers;

import org.testcontainers.utility.DockerImageName;

@Deprecated
public class InfluxDBContainer<SELF extends InfluxDBContainer<SELF>> extends InfluxDBContainerV1<SELF> {
    public InfluxDBContainer(final DockerImageName influxdbTestImage) {
        super(influxdbTestImage);
    }
}
