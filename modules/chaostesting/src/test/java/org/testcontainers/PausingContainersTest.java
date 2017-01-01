package org.testcontainers;

import org.junit.Before;
import org.junit.Test;
import org.testcontainers.DockerEnvironment.ContainerDetails;
import org.testcontainers.containers.GenericContainer;

import java.util.concurrent.TimeUnit;

import static com.jayway.awaitility.Awaitility.await;
import static org.assertj.core.api.Assertions.assertThat;
import static org.testcontainers.PumbaActions.pauseContainersFor;
import static org.testcontainers.PumbaSchedules.onlyOnce;
import static org.testcontainers.PumbaTargets.singleContainer;

/**
 * Created by novy on 31.12.16.
 */
public class PausingContainersTest {

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
                .affectingContainers(singleContainer(containerToPause.getContainerName()))
                .performingAction(pauseContainersFor(10, SupportedTimeUnit.SECONDS))
                .scheduled(onlyOnce());

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

    private GenericContainer startedContainer() {
        final GenericContainer aContainer = new GenericContainer<>("ubuntu:latest")
                .withCommand("bash", "-c", "while true; do echo something; sleep 1; done");
        aContainer.start();
        return aContainer;
    }
}