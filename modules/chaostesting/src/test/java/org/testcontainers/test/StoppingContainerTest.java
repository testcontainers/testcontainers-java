package org.testcontainers.test;

import org.junit.Before;
import org.junit.Test;
import org.testcontainers.PumbaExecutables;
import org.testcontainers.client.PumbaClient;
import org.testcontainers.client.actions.containeractions.ContainerActions;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.test.DockerEnvironment.ContainerDetails;

import java.util.concurrent.TimeUnit;

import static com.jayway.awaitility.Awaitility.await;
import static org.assertj.core.api.Assertions.assertThat;
import static org.testcontainers.client.executionmodes.PumbaExecutionModes.onlyOnce;
import static org.testcontainers.client.targets.PumbaTargets.containers;

/**
 * Created by novy on 31.12.16.
 */
public class StoppingContainerTest implements CanSpawnExampleContainers {

    private DockerEnvironment environment;
    private PumbaClient pumba;

    @Before
    public void setUp() throws Exception {
        environment = new DockerEnvironment();
        pumba = new PumbaClient(PumbaExecutables.dockerized());
    }

    @Test
    public void should_stop_single_container() throws Exception {
        // given
        final GenericContainer containerToStop = startedContainer();

        // when
        pumba
                .performContainerChaos(ContainerActions.stopContainers())
                .affect(containers(containerToStop.getContainerName()))
                .execute(onlyOnce().onAllChosenContainers());

        // then
        await().atMost(30, TimeUnit.SECONDS).until(() -> {
            final ContainerDetails container = environment.containerDetails(containerToStop.getContainerId());
            assertThat(container.isRunning()).isFalse();
        });
    }
}
