package org.testcontainers;

import org.junit.Before;
import org.junit.Test;
import org.testcontainers.DockerEnvironment.ContainerDetails;
import org.testcontainers.containers.GenericContainer;

import java.util.concurrent.TimeUnit;

import static com.jayway.awaitility.Awaitility.await;
import static org.assertj.core.api.Assertions.assertThat;
import static org.testcontainers.PumbaActions.pauseContainersFor;
import static org.testcontainers.PumbaExecutionModes.onlyOnce;
import static org.testcontainers.PumbaTargets.containers;

/**
 * Created by novy on 31.12.16.
 */
public class PausingContainersTest implements CanSpawnExampleContainers {

    private DockerEnvironment environment;

    @Before
    public void setUp() throws Exception {
        environment = new DockerEnvironment();
    }

    @Test
    public void should_pause_single_container() throws Exception {
        // given
        final GenericContainer containerToPause = startedContainer();

        final PumbaContainer pumba = PumbaContainer.newPumba()
                .on(containers(containerToPause.getContainerName()))
                .performAction(pauseContainersFor(10, SupportedTimeUnit.SECONDS))
                .schedule(onlyOnce().withAllContainersAtOnce());

        // when
        pumba.start();

        // then
        await().until(() -> {
            final ContainerDetails container = environment.containerDetails(containerToPause.getContainerId());
            assertThat(container.isPaused()).isTrue();
        });

        await().atMost(15, TimeUnit.SECONDS).until(() -> {
            final ContainerDetails container = environment.containerDetails(containerToPause.getContainerId());
            assertThat(container.isPaused()).isFalse();
        });
    }
}