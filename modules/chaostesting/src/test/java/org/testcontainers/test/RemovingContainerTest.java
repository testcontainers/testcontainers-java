package org.testcontainers.test;

import org.junit.Before;
import org.junit.Test;
import org.testcontainers.PumbaExecutables;
import org.testcontainers.client.PumbaClient;
import org.testcontainers.containers.GenericContainer;

import java.util.concurrent.TimeUnit;

import static com.jayway.awaitility.Awaitility.await;
import static org.assertj.core.api.Assertions.assertThat;
import static org.testcontainers.client.actions.containeractions.ContainerActions.removeContainers;
import static org.testcontainers.client.executionmodes.PumbaExecutionModes.onlyOnce;
import static org.testcontainers.client.targets.PumbaTargets.containers;

/**
 * Created by novy on 31.12.16.
 */

public class RemovingContainerTest implements CanSpawnExampleContainers {

    private DockerEnvironment environment;
    private PumbaClient pumba;

    @Before
    public void setUp() throws Exception {
        environment = new DockerEnvironment();
        pumba = new PumbaClient(PumbaExecutables.dockerized());
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
        await().atMost(30, TimeUnit.SECONDS).until(() ->
                assertThat(environment.namesOfAllContainers())
                        .doesNotContain(containerToRemove.getContainerName())
        );
    }
}
