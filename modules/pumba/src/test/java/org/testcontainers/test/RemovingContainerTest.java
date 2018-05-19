package org.testcontainers.test;

import org.junit.Before;
import org.junit.Test;
import org.testcontainers.client.PumbaClient;
import org.testcontainers.client.PumbaClients;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.executables.PumbaExecutables;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.testcontainers.client.actions.containeractions.ContainerActions.removeContainers;
import static org.testcontainers.client.executionmodes.PumbaExecutionModes.onlyOnce;
import static org.testcontainers.client.targets.PumbaTargets.containers;

/**
 * Created by novy on 31.12.16.
 */

public class RemovingContainerTest implements CanSpawnContainers {

    private DockerEnvironment environment;
    private PumbaClient pumba;

    @Before
    public void setUp() throws Exception {
        environment = new DockerEnvironment();
        pumba = PumbaClients.forExecutable(PumbaExecutables.dockerized());
    }

    @Test
    public void should_remove_running_container() throws Exception {
        // given
        final GenericContainer containerToRemove = startedContainer();

        // when
        pumba
                .performContainerChaos(removeContainers())
                .affect(containers(containerToRemove.getContainerName()))
                .execute(onlyOnce().onAllChosenContainers());

        // then
        await().atMost(30, TimeUnit.SECONDS).untilAsserted(() ->
                assertThat(environment.namesOfAllContainers())
                        .doesNotContain(containerToRemove.getContainerName())
        );
    }
}
