package com.example.linkedcontainer;

import org.testcontainers.utility.DockerImageName;

public interface LinkedContainerTestImages {
    DockerImageName POSTGRES_TEST_IMAGE = DockerImageName.parse("postgres:9.6.12");
    DockerImageName REDMINE_TEST_IMAGE = DockerImageName.parse("redmine:3.3.2");
}
