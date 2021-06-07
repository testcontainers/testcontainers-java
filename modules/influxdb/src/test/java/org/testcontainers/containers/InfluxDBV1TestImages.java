package org.testcontainers.containers;

import org.testcontainers.utility.DockerImageName;

public interface InfluxDBV1TestImages {
    DockerImageName INFLUXDB_TEST_IMAGE = DockerImageName.parse("influxdb:1.4.3");
}
