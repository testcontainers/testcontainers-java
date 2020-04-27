package org.testcontainers.containers;

import org.junit.ClassRule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for basic functionality when used as a <code>@ClassRule</code>.
 *
 * @author Mrtin Greber
 */
public class ActiveMQContainerJUnitIntegrationTest {

    @ClassRule
    public static ActiveMQContainer activeMQContainer = new ActiveMQContainer();

    @Test
    public void shouldStart() {
        assertThat(activeMQContainer.isRunning()).isTrue();
    }
}
