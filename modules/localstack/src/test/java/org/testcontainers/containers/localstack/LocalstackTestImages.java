package org.testcontainers.containers.localstack;

import org.testcontainers.utility.DockerImageName;

public interface LocalstackTestImages {
    DockerImageName LOCALSTACK_IMAGE = new DockerImageName("localstack/localstack:0.10.8");
    DockerImageName AWS_CLI_IMAGE = new DockerImageName("atlassian/pipelines-awscli:1.16.302");
}
