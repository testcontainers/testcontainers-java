package org.testcontainers.test;

import org.junit.Before;
import org.junit.Test;
import org.testcontainers.PumbaExecutables;
import org.testcontainers.client.PumbaClient;
import org.testcontainers.client.commandparts.SupportedTimeUnit;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.test.DockerEnvironment.ContainerDetails;

import java.util.concurrent.TimeUnit;

import static com.jayway.awaitility.Awaitility.await;
import static org.assertj.core.api.Assertions.assertThat;
import static org.testcontainers.client.actions.containeractions.ContainerActions.pauseContainers;
import static org.testcontainers.client.executionmodes.PumbaExecutionModes.onlyOnce;
import static org.testcontainers.client.targets.PumbaTargets.containers;

/**
 * Created by novy on 31.12.16.
 */
public class PausingContainersTest implements CanSpawnExampleContainers {

    private DockerEnvironment environment;
    private PumbaClient pumba;

    @Before
    public void setUp() throws Exception {
        environment = new DockerEnvironment();
        pumba = new PumbaClient(PumbaExecutables.dockerized());
    }

    @Test
    public void should_pause_single_container() throws Exception {
        // given
        final GenericContainer containerToPause = startedContainer();

        // when
        pumba
                .performContainerChaos(pauseContainers().forDuration(1, SupportedTimeUnit.SECONDS))
                .affect(containers(containerToPause.getContainerName()))
                .execute(onlyOnce().onAllChosenContainers());

        // then
        await().atMost(1, TimeUnit.SECONDS).until(() -> {
            final ContainerDetails container = environment.containerDetails(containerToPause.getContainerId());
            assertThat(container.isPaused()).isTrue();
        });

        await().atMost(1, TimeUnit.SECONDS).until(() -> {
            final ContainerDetails container = environment.containerDetails(containerToPause.getContainerId());
            assertThat(container.isPaused()).isFalse();
        });
    }
}
