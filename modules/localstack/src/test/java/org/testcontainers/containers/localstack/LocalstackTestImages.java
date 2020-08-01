package org.testcontainers.containers.localstack;

import org.testcontainers.utility.DockerImageName;

public interface LocalstackTestImages {
    DockerImageName LOCALSTACK_IMAGE = DockerImageName.parse("localstack/localstack:0.11.0");
    DockerImageName AWS_CLI_IMAGE = DockerImageName.parse("atlassian/pipelines-awscli:1.16.302");
}
