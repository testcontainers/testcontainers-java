package org.testcontainers.containers;

import org.testcontainers.utility.DockerImageName;

public interface InfluxDBTestImages {
    DockerImageName INFLUXDB_TEST_IMAGE = DockerImageName.parse("quay.io/influxdb/influxdb:v2.0.0");
}
