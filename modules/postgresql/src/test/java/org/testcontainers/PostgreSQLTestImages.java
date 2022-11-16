package org.testcontainers;

import org.testcontainers.utility.DockerImageName;

public interface PostgreSQLTestImages {
    DockerImageName POSTGRES_TEST_IMAGE = DockerImageName.parse("postgres:9.6.12");
}
