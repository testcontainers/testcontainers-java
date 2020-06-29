package org.testcontainers.containers;

import org.testcontainers.utility.DockerImageName;

public interface InfluxDBTestImages {
    DockerImageName INFLUXDB_TEST_IMAGE = new DockerImageName("influxdb:1.4.3");
}
