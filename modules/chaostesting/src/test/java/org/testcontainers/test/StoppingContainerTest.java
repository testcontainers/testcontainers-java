package org.testcontainers.test;

import org.junit.Before;
import org.junit.Test;
import org.testcontainers.ContainerActions;
import org.testcontainers.PumbaContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.test.DockerEnvironment.ContainerDetails;

import java.util.concurrent.TimeUnit;

import static com.jayway.awaitility.Awaitility.await;
import static org.assertj.core.api.Assertions.assertThat;
import static org.testcontainers.PumbaExecutionModes.onlyOnce;
import static org.testcontainers.PumbaTargets.containers;

/**
 * Created by novy on 31.12.16.
 */
public class StoppingContainerTest extends ShutdownsOrphanedContainers implements CanSpawnExampleContainers {

    private DockerEnvironment environment;

    @Before
    public void setUp() throws Exception {
        environment = new DockerEnvironment();
    }

    @Test
    public void should_stop_single_container() throws Exception {
        // given
        final GenericContainer containerToStop = startedContainer();

        final GenericContainer<PumbaContainer> pumba = PumbaContainer.newPumba()
                .performContainerChaos(ContainerActions.stopContainers())
                .affect(containers(containerToStop.getContainerName()))
                .execute(onlyOnce().onAllChosenContainers());

        // when
        pumba.start();

        // then
        await().atMost(30, TimeUnit.SECONDS).until(() -> {
            final ContainerDetails container = environment.containerDetails(containerToStop.getContainerId());
            assertThat(container.isRunning()).isFalse();
        });
    }
}
