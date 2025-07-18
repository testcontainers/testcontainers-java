package org.testcontainers;

import org.testcontainers.utility.DockerImageName;

public interface ClickhouseTestImages {
    DockerImageName CLICKHOUSE_IMAGE = DockerImageName.parse("clickhouse/clickhouse-server:21.11.11-alpine");

    DockerImageName CLICKHOUSE_24_12_IMAGE = DockerImageName.parse("clickhouse/clickhouse-server:24.12-alpine");
}
