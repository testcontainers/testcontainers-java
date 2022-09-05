package org.testcontainers.containers;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.testcontainers.junit4.ClassContainer;
import org.testcontainers.junit4.TestContainersRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for basic functionality when used as a <code>@ClassContainer</code>.
 */
@RunWith(TestContainersRunner.class)
public class RabbitMQContainerJUnitIntegrationTest {

    @ClassContainer
    public static RabbitMQContainer rabbitMQContainer = new RabbitMQContainer(RabbitMQTestImages.RABBITMQ_IMAGE);

    @Test
    public void shouldStart() {
        assertThat(rabbitMQContainer.isRunning()).isTrue();
    }
}
