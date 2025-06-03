package org.testcontainers.containers;

import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
public class RabbitMQContainerJUnitIntegrationTest {

    @Container
    public static RabbitMQContainer rabbitMQContainer = new RabbitMQContainer(RabbitMQTestImages.RABBITMQ_IMAGE);

    @Test
    public void shouldStart() {
        assertThat(rabbitMQContainer.isRunning()).isTrue();
    }
}
