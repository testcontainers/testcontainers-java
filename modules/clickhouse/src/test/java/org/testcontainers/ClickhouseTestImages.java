package org.testcontainers;

import org.testcontainers.utility.DockerImageName;

public interface ClickhouseTestImages {
    DockerImageName CLICKHOUSE_IMAGE = Boolean.getBoolean("clickhouse-temporarily-use-deprecated-driver")
        ? DockerImageName.parse("yandex/clickhouse-server:18.10.3")
        : DockerImageName.parse("clickhouse/clickhouse-server:21.3");
}
