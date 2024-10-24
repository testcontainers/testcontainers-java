package org.testcontainers;

import org.testcontainers.utility.DockerImageName;

public interface ClickhouseTestImages {
    DockerImageName CLICKHOUSE_IMAGE = DockerImageName.parse("clickhouse/clickhouse-server:21.9.2-alpine");
}
