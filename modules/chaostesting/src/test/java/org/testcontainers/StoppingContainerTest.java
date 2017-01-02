package org.testcontainers;

import org.junit.Before;
import org.junit.Test;
import org.testcontainers.DockerEnvironment.ContainerDetails;
import org.testcontainers.containers.GenericContainer;

import java.util.concurrent.TimeUnit;

import static com.jayway.awaitility.Awaitility.await;
import static org.assertj.core.api.Assertions.assertThat;
import static org.testcontainers.PumbaActions.stopContainers;
import static org.testcontainers.PumbaExecutionModes.onlyOnce;
import static org.testcontainers.PumbaTargets.containers;

/**
 * Created by novy on 31.12.16.
 */
public class StoppingContainerTest implements CanSpawnExampleContainers {

    private DockerEnvironment environment;

    @Before
    public void setUp() throws Exception {
        environment = new DockerEnvironment();
    }

    @Test
    public void should_stop_single_container() throws Exception {
        // given
        final GenericContainer containerToStop = startedContainer();

        final PumbaContainer pumba = PumbaContainer.newPumba()
                .on(containers(containerToStop.getContainerName()))
                .performAction(stopContainers())
                .schedule(onlyOnce().withAllContainersAtOnce());

        // when
        pumba.start();

        // then
        await().atMost(20, TimeUnit.SECONDS).until(() -> {
            final ContainerDetails container = environment.containerDetails(containerToStop.getContainerId());
            assertThat(container.isRunning()).isFalse();
        });
    }
}