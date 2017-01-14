package org.testcontainers.test;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.testcontainers.PumbaContainer;
import org.testcontainers.containers.GenericContainer;

import static com.jayway.awaitility.Awaitility.await;
import static org.assertj.core.api.Assertions.assertThat;
import static org.testcontainers.ContainerActions.removeContainers;
import static org.testcontainers.PumbaExecutionModes.onlyOnce;
import static org.testcontainers.PumbaTargets.containers;

/**
 * Created by novy on 31.12.16.
 */

@Ignore
// todo: there are some issues on pumba side, mentioned in every test case
public class RemovingContainerTest implements CanSpawnExampleContainers {

    private DockerEnvironment environment;

    @Before
    public void setUp() throws Exception {
        environment = new DockerEnvironment();
    }

    @Test
    // todo https://github.com/gaia-adm/pumba/issues/30
    public void should_remove_stopped_container() throws Exception {
        // given
        final GenericContainer stoppedContainerToRemove = stoppedContainer();

        final GenericContainer<PumbaContainer> pumba = PumbaContainer.newPumba()
                .performContainerChaos(removeContainers())
                .affect(containers(stoppedContainerToRemove.getContainerName()))
                .execute(onlyOnce().onAllChosenContainers());

        // when
        pumba.start();

        // then
        await().until(() ->
                assertThat(environment.namesOfAllContainers())
                        .doesNotContain(stoppedContainerToRemove.getContainerName())
        );

    }

    @Test
    // todo https://github.com/gaia-adm/pumba/issues/31
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
        await().until(() ->
                assertThat(environment.namesOfAllContainers())
                        .doesNotContain(containerToRemove.getContainerName())
        );
    }
}
