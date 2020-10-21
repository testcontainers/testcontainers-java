package org.testcontainers;

import org.testcontainers.utility.DockerImageName;

public interface PostgreSQLTestImages {
    String POSTGRES_VERSION = "postgres:9.6";
    String POSTGRES_ALPINE_VERSION = "postgres:9.6-alpine";

    DockerImageName POSTGRES_TEST_IMAGE = DockerImageName.parse(POSTGRES_VERSION);;
}
