package org.testcontainers.rabbitmq;

import org.testcontainers.utility.DockerImageName;

public interface RabbitMQTestImages {
    DockerImageName RABBITMQ_IMAGE = DockerImageName.parse("rabbitmq:3.7.25-management-alpine");
}
