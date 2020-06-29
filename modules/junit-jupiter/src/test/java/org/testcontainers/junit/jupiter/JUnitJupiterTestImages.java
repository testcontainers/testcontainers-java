package org.testcontainers.junit.jupiter;

import org.testcontainers.utility.DockerImageName;

public interface JUnitJupiterTestImages {
    DockerImageName POSTGRES_IMAGE = new DockerImageName("postgres:9.6.12");
    DockerImageName HTTPD_IMAGE = new DockerImageName("httpd:2.4-alpine");
}
