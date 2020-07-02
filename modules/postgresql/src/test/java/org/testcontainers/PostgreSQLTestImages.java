package org.testcontainers;

import org.testcontainers.utility.DockerImageName;

public interface PostgreSQLTestImages {
    DockerImageName POSTGRES_TEST_IMAGE = new DockerImageName("postgres:9.6.12");
}
