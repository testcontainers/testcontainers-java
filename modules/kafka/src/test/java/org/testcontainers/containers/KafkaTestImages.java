package org.testcontainers.containers;

import org.testcontainers.utility.DockerImageName;

public interface KafkaTestImages {
    DockerImageName KAFKA_TEST_IMAGE = new DockerImageName("confluentinc/cp-kafka:5.2.1");
    DockerImageName ZOOKEEPER_TEST_IMAGE = new DockerImageName("confluentinc/cp-zookeeper:4.0.0");
}
