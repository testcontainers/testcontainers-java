package org.testcontainers.test;

import org.junit.Before;
import org.junit.Test;
import org.testcontainers.client.PumbaClient;
import org.testcontainers.client.PumbaClients;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.executables.PumbaExecutables;
import org.testcontainers.test.DockerEnvironment.ContainerDetails;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.testcontainers.client.actions.containeractions.ContainerActions.pauseContainers;
import static org.testcontainers.client.executionmodes.PumbaExecutionModes.onlyOnce;
import static org.testcontainers.client.targets.PumbaTargets.containers;

/**
 * Created by novy on 31.12.16.
 */
public class PausingContainersTest implements CanSpawnContainers {

    private DockerEnvironment environment;
    private PumbaClient pumba;

    @Before
    public void setUp() throws Exception {
        environment = new DockerEnvironment();
        pumba = PumbaClients.forExecutable(PumbaExecutables.dockerized());
    }

    @Test
    public void should_pause_single_container() throws Exception {
        // given
        final GenericContainer containerToPause = startedContainer();

        // when
        pumba
                .performContainerChaos(pauseContainers().forDuration(Duration.ofSeconds(1)))
                .affect(containers(containerToPause.getContainerName()))
                .execute(onlyOnce().onAllChosenContainers());

        // then
        await().atMost(1, TimeUnit.SECONDS).untilAsserted(() -> {
            final ContainerDetails container = environment.containerDetails(containerToPause.getContainerId());
            assertThat(container.isPaused()).isTrue();
        });

        await().atMost(1, TimeUnit.SECONDS).untilAsserted(() -> {
            final ContainerDetails container = environment.containerDetails(containerToPause.getContainerId());
            assertThat(container.isPaused()).isFalse();
        });
    }
}
