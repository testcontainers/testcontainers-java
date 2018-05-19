package org.testcontainers.test;

import org.junit.Before;
import org.junit.Test;
import org.testcontainers.client.PumbaClient;
import org.testcontainers.client.PumbaClients;
import org.testcontainers.client.commandparts.SupportedTimeUnit;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.executables.PumbaExecutables;

import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.testcontainers.client.actions.containeractions.ContainerActions.killContainers;
import static org.testcontainers.client.executionmodes.PumbaExecutionModes.onlyOnce;
import static org.testcontainers.client.executionmodes.PumbaExecutionModes.recurrently;
import static org.testcontainers.client.targets.PumbaTargets.containers;
import static org.testcontainers.client.targets.PumbaTargets.containersMatchingRegexp;

/**
 * Created by novy on 31.12.16.
 */
public class KillingContainersTest implements CanSpawnContainers {

    private DockerEnvironment environment;
    private PumbaClient pumba;

    @Before
    public void setUp() throws Exception {
        environment = new DockerEnvironment();
        pumba = PumbaClients.forExecutable(PumbaExecutables.dockerized());
    }

    @Test
    public void should_kill_single_container() throws Exception {
        // given
        final GenericContainer containerToKill = startedContainer();
        final GenericContainer containerThatShouldSurvive = startedContainer();

        // when
        pumba
                .performContainerChaos(killContainers())
                .affect(containers(containerToKill.getContainerName()))
                .execute(onlyOnce().onAllChosenContainers());

        // then
        await().atMost(30, TimeUnit.SECONDS).untilAsserted(() -> {
            final Collection<String> namesOfRunningContainers = environment.namesOfRunningContainers();

            assertThat(namesOfRunningContainers).doesNotContain(containerToKill.getContainerName());
            assertThat(namesOfRunningContainers).contains(containerThatShouldSurvive.getContainerName());
        });
    }

    @Test
    public void should_kill_more_than_one_containers() throws Exception {
        // given
        final GenericContainer firstVictim = startedContainer();
        final GenericContainer secondVictim = startedContainer();
        final GenericContainer survivor = startedContainer();

        // when
        pumba
                .performContainerChaos(killContainers())
                .affect(containers(firstVictim.getContainerName(), secondVictim.getContainerName()))
                .execute(onlyOnce().onAllChosenContainers());

        // then
        await().atMost(30, TimeUnit.SECONDS).untilAsserted(() -> {
            final Collection<String> namesOfRunningContainers = environment.namesOfRunningContainers();

            assertThat(namesOfRunningContainers).doesNotContain(firstVictim.getContainerName(), secondVictim.getContainerName());
            assertThat(namesOfRunningContainers).contains(survivor.getContainerName());
        });
    }

    @Test
    public void should_kill_containers_matching_regular_expression() throws Exception {
        // given
        startedContainerWithName(containerNameStartingWith("foobar"));
        startedContainerWithName(containerNameStartingWith("foobar"));
        startedContainerWithName(containerNameStartingWith("barbaz"));

        // when
        pumba
                .performContainerChaos(killContainers())
                .affect(containersMatchingRegexp("foobar.*"))
                .execute(onlyOnce().onAllChosenContainers());

        // then
        await().atMost(30, TimeUnit.SECONDS).untilAsserted(() -> {
            final Collection<String> namesOfRunningContainers = environment.namesOfRunningContainers();

            assertThat(namesOfRunningContainers).filteredOn(matchesRegexp("foobar.*")).isEmpty();
            assertThat(namesOfRunningContainers).filteredOn(matchesRegexp("barbaz.*")).isNotEmpty();
        });
    }

    @Test
    public void should_kill_one_random_container_periodically() throws Exception {
        // given
        startedContainerWithName(containerNameStartingWith("foobar"));
        startedContainerWithName(containerNameStartingWith("foobar"));

        // when
        pumba
                .performContainerChaos(killContainers())
                .affect(containersMatchingRegexp("foobar.*"))
                .execute(recurrently(5, SupportedTimeUnit.SECONDS).onRandomlyChosenContainer());

        // then
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() ->
                assertThat(environment.namesOfRunningContainers())
                        .filteredOn(matchesRegexp("foobar.*"))
                        .hasSize(1)
        );

        // and
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() ->
                assertThat(environment.namesOfRunningContainers())
                        .filteredOn(matchesRegexp("foobar.*"))
                        .isEmpty()
        );
    }

    private Predicate<String> matchesRegexp(String regexp) {
        return Pattern.compile(regexp).asPredicate();
    }

    private String containerNameStartingWith(String prefix) {
        return prefix + "_" + UUID.randomUUID().toString();
    }
}
