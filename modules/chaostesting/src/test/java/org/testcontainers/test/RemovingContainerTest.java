package org.testcontainers.test;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.testcontainers.PumbaContainer;
import org.testcontainers.containers.GenericContainer;

import java.util.concurrent.TimeUnit;

import static com.jayway.awaitility.Awaitility.await;
import static org.assertj.core.api.Assertions.assertThat;
import static org.testcontainers.ContainerActions.removeContainers;
import static org.testcontainers.PumbaExecutionModes.onlyOnce;
import static org.testcontainers.PumbaTargets.containers;

/**
 * Created by novy on 31.12.16.
 */

public class RemovingContainerTest extends ShutdownsOrphanedContainers implements CanSpawnExampleContainers {

    private DockerEnvironment environment;

    @Before
    public void setUp() throws Exception {
        environment = new DockerEnvironment();
    }

    @Test
    public void should_remove_running_container() throws Exception {
        // given
        final GenericContainer containerToRemove = startedContainer();

        final GenericContainer<PumbaContainer> pumba = PumbaContainer.newPumba()
                .performContainerChaos(removeContainers())
                .affect(containers(containerToRemove.getContainerName()))
                .execute(onlyOnce().onAllChosenContainers());

        // when
        pumba.start();

        // then
        await().atMost(30, TimeUnit.SECONDS).until(() ->
                assertThat(environment.namesOfAllContainers())
                        .doesNotContain(containerToRemove.getContainerName())
        );
    }
}
