package org.testcontainers.junit.jqwik;

import org.testcontainers.utility.DockerImageName;

public interface JqwikTestImages {
    DockerImageName POSTGRES_IMAGE = DockerImageName.parse("postgres:9.6.12");
    DockerImageName HTTPD_IMAGE = DockerImageName.parse("httpd:2.4-alpine");
}
