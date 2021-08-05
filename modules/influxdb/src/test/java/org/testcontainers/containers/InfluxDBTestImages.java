package org.testcontainers.containers;

import org.testcontainers.utility.DockerImageName;

public interface InfluxDBTestImages {
    DockerImageName INFLUXDB_V1_TEST_IMAGE = DockerImageName.parse("influxdb:1.4.3");
    DockerImageName INFLUXDB_V2_TEST_IMAGE = DockerImageName.parse("influxdb:2.0.7");
}
