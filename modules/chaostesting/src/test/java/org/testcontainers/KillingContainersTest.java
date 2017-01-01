package org.testcontainers;

import org.junit.Before;
import org.junit.Test;
import org.testcontainers.containers.GenericContainer;

import java.util.Collection;

import static com.jayway.awaitility.Awaitility.await;
import static org.assertj.core.api.Assertions.assertThat;
import static org.testcontainers.PumbaActions.killContainers;
import static org.testcontainers.PumbaSchedules.onlyOnce;
import static org.testcontainers.PumbaTargets.allContainers;
import static org.testcontainers.PumbaTargets.singleContainer;

/**
 * Created by novy on 31.12.16.
 */
public class KillingContainersTest {

    private DockerEnvironment environment;

    @Before
    public void setUp() throws Exception {
        environment = new DockerEnvironment();
    }

    @Test
    public void should_kill_all_containers() throws Exception {
        // given
        startedContainer();
        startedContainer();

        final PumbaContainer pumba = PumbaContainer.newPumba()
                .affectingContainers(allContainers())
                .performingAction(killContainers())
                .scheduled(onlyOnce());

        // when
        pumba.start();

        // then
        await().until(() -> assertThat(environment.namesOfRunningContainers()).isEmpty());
    }

    @Test
    public void should_kill_single_container() throws Exception {
        // given
        final GenericContainer containerToKill = startedContainer();
        final GenericContainer containerThatShouldSurvive = startedContainer();

        final PumbaContainer pumba = PumbaContainer.newPumba()
                .affectingContainers(singleContainer(containerToKill.getContainerName()))
                .performingAction(killContainers())
                .scheduled(onlyOnce());

        // when
        pumba.start();

        // then
        await().until(() -> {
            final Collection<String> namesOfRunningContainers = environment.namesOfRunningContainers();
            assertThat(namesOfRunningContainers).doesNotContain(containerToKill.getContainerName());
            assertThat(namesOfRunningContainers).contains(containerThatShouldSurvive.getContainerName());
        });
    }


    private GenericContainer startedContainer() {
        final GenericContainer aContainer = new GenericContainer<>("ubuntu:latest")
                .withCommand("bash", "-c", "while true; do echo something; sleep 1; done");
        aContainer.start();
        return aContainer;
    }


}