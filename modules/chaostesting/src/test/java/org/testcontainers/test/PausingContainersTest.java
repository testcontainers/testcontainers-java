package org.testcontainers.test;

import org.junit.Before;
import org.junit.Test;
import org.testcontainers.PumbaContainer;
import org.testcontainers.SupportedTimeUnit;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.test.DockerEnvironment.ContainerDetails;

import java.util.concurrent.TimeUnit;

import static com.jayway.awaitility.Awaitility.await;
import static org.assertj.core.api.Assertions.assertThat;
import static org.testcontainers.ContainerActions.pauseContainers;
import static org.testcontainers.PumbaExecutionModes.onlyOnce;
import static org.testcontainers.PumbaTargets.containers;

/**
 * Created by novy on 31.12.16.
 */
public class PausingContainersTest extends ShutdownsOrphanedContainers implements CanSpawnExampleContainers {

    private DockerEnvironment environment;

    @Before
    public void setUp() throws Exception {
        environment = new DockerEnvironment();
    }

    @Test
    public void should_pause_single_container() throws Exception {
        // given
        final GenericContainer containerToPause = startedContainer();

        final GenericContainer<PumbaContainer> pumba = PumbaContainer.newPumba()
                .performContainerChaos(pauseContainers().forDuration(10, SupportedTimeUnit.SECONDS))
                .affect(containers(containerToPause.getContainerName()))
                .execute(onlyOnce().onAllChosenContainers());

        // when
        pumba.start();

        // then
        await().atMost(20, TimeUnit.SECONDS).until(() -> {
            final ContainerDetails container = environment.containerDetails(containerToPause.getContainerId());
            assertThat(container.isPaused()).isTrue();
        });

        await().atMost(15, TimeUnit.SECONDS).until(() -> {
            final ContainerDetails container = environment.containerDetails(containerToPause.getContainerId());
            assertThat(container.isPaused()).isFalse();
        });
    }
}
