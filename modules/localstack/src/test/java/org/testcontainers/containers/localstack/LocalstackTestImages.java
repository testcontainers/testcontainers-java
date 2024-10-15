package org.testcontainers.containers.localstack;

import org.testcontainers.utility.DockerImageName;

public interface LocalstackTestImages {
    DockerImageName LOCALSTACK_IMAGE = DockerImageName.parse("localstack/localstack:0.12.8");
    DockerImageName LOCALSTACK_0_10_IMAGE = LOCALSTACK_IMAGE.withTag("0.10.7");
    DockerImageName LOCALSTACK_0_11_IMAGE = LOCALSTACK_IMAGE.withTag("0.11.3");
    DockerImageName LOCALSTACK_0_12_IMAGE = LOCALSTACK_IMAGE.withTag("0.12.8");
    DockerImageName LOCALSTACK_0_13_IMAGE = LOCALSTACK_IMAGE.withTag("0.13.0");

    DockerImageName LOCALSTACK_2_3_IMAGE = LOCALSTACK_IMAGE.withTag("2.3");
    DockerImageName AWS_CLI_IMAGE = DockerImageName.parse("amazon/aws-cli:2.7.27");
}
