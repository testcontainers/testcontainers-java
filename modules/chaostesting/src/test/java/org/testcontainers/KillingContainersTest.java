package org.testcontainers;

import org.junit.Before;
import org.junit.Test;
import org.testcontainers.containers.GenericContainer;

import java.util.Collection;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import static com.jayway.awaitility.Awaitility.await;
import static org.assertj.core.api.Assertions.assertThat;
import static org.testcontainers.PumbaActions.killContainers;
import static org.testcontainers.PumbaSchedules.onlyOnce;
import static org.testcontainers.PumbaTargets.*;

/**
 * Created by novy on 31.12.16.
 */
public class KillingContainersTest implements CanSpawnExampleContainers {

    private DockerEnvironment environment;

    @Before
    public void setUp() throws Exception {
        environment = new DockerEnvironment();
    }

    @Test
    public void should_kill_single_container() throws Exception {
        // given
        final GenericContainer containerToKill = startedContainer();
        final GenericContainer containerThatShouldSurvive = startedContainer();

        final PumbaContainer pumba = PumbaContainer.newPumba()
                .affectingContainers(containers(containerToKill.getContainerName()))
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

    @Test
    public void should_kill_more_than_one_containers() throws Exception {
        // given
        final GenericContainer firstVictim = startedContainer();
        final GenericContainer secondVictim = startedContainer();
        final GenericContainer survivor = startedContainer();

        final PumbaContainer pumba = PumbaContainer.newPumba()
                .affectingContainers(containers(firstVictim.getContainerName(), secondVictim.getContainerName()))
                .performingAction(killContainers())
                .scheduled(onlyOnce());

        // when
        pumba.start();

        // then
        await().until(() -> {
            final Collection<String> namesOfRunningContainers = environment.namesOfRunningContainers();

            assertThat(namesOfRunningContainers).doesNotContain(firstVictim.getContainerName(), secondVictim.getContainerName());
            assertThat(namesOfRunningContainers).contains(survivor.getContainerName());
        });
    }

    @Test
    public void should_kill_containers_matching_regular_expression() throws Exception {
        // given
        startContainerWithNameContaining("foobar");
        startContainerWithNameContaining("foobar");
        startContainerWithNameContaining("barbaz");

        final PumbaContainer pumba = PumbaContainer.newPumba()
                .affectingContainers(containersMatchingRegexp(".*foobar.*"))
                .performingAction(killContainers())
                .scheduled(onlyOnce());

        // when
        pumba.start();

        // then
        await().until(() -> {
            final Collection<String> namesOfRunningContainers = environment.namesOfRunningContainers();

            assertThat(namesOfRunningContainers).filteredOn(matchesRegexp(".*foobar.*")).isEmpty();
            assertThat(namesOfRunningContainers).filteredOn(matchesRegexp(".*barbaz.*")).isNotEmpty();
        });
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

    private Predicate<String> matchesRegexp(String regexp) {
        return Pattern.compile(regexp).asPredicate();
    }
}
