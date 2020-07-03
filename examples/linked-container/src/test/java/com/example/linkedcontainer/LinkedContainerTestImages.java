package com.example.linkedcontainer;

import org.testcontainers.utility.DockerImageName;

public interface LinkedContainerTestImages {
    DockerImageName POSTGRES_TEST_IMAGE = DockerImageName.of("postgres:9.6.12");
    DockerImageName REDMINE_TEST_IMAGE = DockerImageName.of("redmine:3.3.2");
}
