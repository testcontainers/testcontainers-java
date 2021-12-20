package org.testcontainers.containers.localstack;

import org.testcontainers.utility.DockerImageName;

public interface LocalstackTestImages {
    DockerImageName LOCALSTACK_IMAGE = DockerImageName.parse("localstack/localstack:0.12.8");
    DockerImageName LOCALSTACK_0_7_IMAGE = LOCALSTACK_IMAGE.withTag("0.7.0");
    DockerImageName LOCALSTACK_0_10_IMAGE = LOCALSTACK_IMAGE.withTag("0.10.7");
    DockerImageName LOCALSTACK_0_11_IMAGE = LOCALSTACK_IMAGE.withTag("0.11.3");
    DockerImageName LOCALSTACK_0_12_IMAGE = LOCALSTACK_IMAGE.withTag("0.12.8");
    DockerImageName AWS_CLI_IMAGE = DockerImageName.parse("atlassian/pipelines-awscli:1.16.302");
}
