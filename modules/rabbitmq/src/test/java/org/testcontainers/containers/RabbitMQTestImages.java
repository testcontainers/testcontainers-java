package org.testcontainers.containers;

import org.testcontainers.utility.DockerImageName;

public interface RabbitMQTestImages {
    DockerImageName RABBITMQ_IMAGE_3_7 = DockerImageName.parse("rabbitmq:3.7-management");
    DockerImageName RABBITMQ_IMAGE_3_9 = DockerImageName.parse("rabbitmq:3.9-management");
    DockerImageName RABBITMQ_IMAGE_3_12 = DockerImageName.parse("rabbitmq:3.12-management");
}
