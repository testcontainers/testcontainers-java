package org.testcontainers.junit.jupiter;

import org.testcontainers.utility.DockerImageName;

public interface JUnitJupiterTestImages {
    DockerImageName POSTGRES_IMAGE = DockerImageName.parse("postgres:9.6.12");
    DockerImageName HTTPD_IMAGE = DockerImageName.parse("httpd:2.4-alpine");
}
