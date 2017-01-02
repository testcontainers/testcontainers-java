package org.testcontainers;

import org.junit.Before;
import org.junit.Test;
import org.testcontainers.containers.GenericContainer;

import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import static com.jayway.awaitility.Awaitility.await;
import static org.assertj.core.api.Assertions.assertThat;
import static org.testcontainers.PumbaActions.killContainers;
import static org.testcontainers.PumbaExecutionModes.onlyOnce;
import static org.testcontainers.PumbaExecutionModes.recurrently;
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
                .performAction(killContainers())
                .on(containers(containerToKill.getContainerName()))
                .schedule(onlyOnce().withAllContainersAtOnce());

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
                .on(containers(firstVictim.getContainerName(), secondVictim.getContainerName()))
                .performAction(killContainers())
                .schedule(onlyOnce().withAllContainersAtOnce());

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
                .on(containersMatchingRegexp(".*foobar.*"))
                .performAction(killContainers())
                .schedule(onlyOnce().withAllContainersAtOnce());

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
                .on(allContainers())
                .performAction(killContainers())
                .schedule(onlyOnce().withAllContainersAtOnce());

        // when
        pumba.start();

        // then
        await().until(() -> assertThat(environment.namesOfRunningContainers()).isEmpty());
    }

    @Test
    public void should_kill_one_random_container_periodically() throws Exception {
        // given
        startContainerWithNameContaining("foobar");
        startContainerWithNameContaining("foobar");

        final PumbaContainer pumba = PumbaContainer.newPumba()
                .on(containersMatchingRegexp(".*foobar.*"))
                .performAction(killContainers())
                .schedule(recurrently(5, SupportedTimeUnit.SECONDS).withOneContainerAtTime());

        // when
        pumba.start();

        // then
        await().atMost(8, TimeUnit.SECONDS).until(() ->
                assertThat(environment.namesOfRunningContainers())
                        .filteredOn(matchesRegexp(".*foobar.*"))
                        .hasSize(1)
        );

        // and
        await().atMost(8, TimeUnit.SECONDS).until(() ->
                assertThat(environment.namesOfRunningContainers())
                        .filteredOn(matchesRegexp(".*foobar.*"))
                        .isEmpty()
        );
    }

    private Predicate<String> matchesRegexp(String regexp) {
        return Pattern.compile(regexp).asPredicate();
    }
}
