package org.testcontainers;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.testcontainers.containers.GenericContainer;

import static com.jayway.awaitility.Awaitility.await;
import static org.assertj.core.api.Assertions.assertThat;
import static org.testcontainers.PumbaActions.removeContainers;
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

        final PumbaContainer pumba = PumbaContainer.newPumba()
                .on(containers(stoppedContainerToRemove.getContainerName()))
                .performAction(removeContainers())
                .schedule(onlyOnce().withAllContainersAtOnce());

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

        final PumbaContainer pumba = PumbaContainer.newPumba()
                .on(containers(containerToRemove.getContainerName()))
                .performAction(removeContainers())
                .schedule(onlyOnce().withAllContainersAtOnce());

        // when
        pumba.start();

        // then
        await().until(() ->
                assertThat(environment.namesOfAllContainers())
                        .doesNotContain(containerToRemove.getContainerName())
        );
    }
}