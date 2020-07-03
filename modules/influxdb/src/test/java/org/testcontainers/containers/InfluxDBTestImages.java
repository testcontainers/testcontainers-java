package org.testcontainers.containers;

import org.testcontainers.utility.DockerImageName;

public interface InfluxDBTestImages {
    DockerImageName INFLUXDB_TEST_IMAGE = DockerImageName.parse("influxdb:1.4.3");
}
