package org.testcontainers.containers;

import org.junit.ClassRule;
import org.junit.Test;
import org.testcontainers.junit4.TestcontainersRule;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for basic functionality when used as a <code>@ClassRule</code>.
 */
public class RabbitMQContainerJUnitIntegrationTest {

    @ClassRule
    public static TestcontainersRule<RabbitMQContainer> rabbitMQContainer = new TestcontainersRule<>(
        new RabbitMQContainer(RabbitMQTestImages.RABBITMQ_IMAGE)
    );

    @Test
    public void shouldStart() {
        assertThat(rabbitMQContainer.get().isRunning()).isTrue();
    }
}
