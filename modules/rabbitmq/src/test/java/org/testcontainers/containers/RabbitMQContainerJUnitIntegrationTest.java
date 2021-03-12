package org.testcontainers.containers;

import org.junit.ClassRule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for basic functionality when used as a <code>@ClassRule</code>.
 *
 * @author Mrtin Greber
 */
public class RabbitMQContainerJUnitIntegrationTest {

    @ClassRule
    public static RabbitMQContainer rabbitMQContainer = new RabbitMQContainer(RabbitMQTestImages.RABBITMQ_IMAGE);

    @Test
    public void shouldStart() {
        assertThat(rabbitMQContainer.isRunning()).isTrue();
    }
}
