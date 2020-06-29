package org.testcontainers;

import org.testcontainers.utility.DockerImageName;

public class PostgreSQLTestImages {
    public static final DockerImageName POSTGRES_TEST_IMAGE = new DockerImageName("postgres:9.6.12");
}
