package org.testcontainers.test;

import org.junit.Test;
import org.testcontainers.PumbaContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.test.Network.CanPingContainers;
import org.testcontainers.test.Network.PingResponse;

import java.util.concurrent.TimeUnit;

import static com.jayway.awaitility.Awaitility.await;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.testcontainers.NetworkActions.anAction;
import static org.testcontainers.NetworkSubCommands.rateLimitOutgoingTraffic;
import static org.testcontainers.PumbaExecutionModes.onlyOnce;
import static org.testcontainers.PumbaTargets.containers;
import static org.testcontainers.RateLimitOutgoingTraffic.RateUnit.KILOBITS_PER_SECOND;
import static org.testcontainers.SupportedTimeUnit.MINUTES;

/**
 * Created by novy on 15.01.17.
 */
public class RateLimitingOutgoingPacketsTest extends ShutdownsOrphanedContainers implements CanSpawnExampleContainers, CanPingContainers {

    @Test
    public void should_be_able_to_rate_limit_outgoing_packets_from_container() throws Exception {
        // given
        final GenericContainer containerToRateLimit = startedContainer();

        final GenericContainer<PumbaContainer> pumba = PumbaContainer.newPumba()
                .performNetworkChaos(
                        anAction()
                                .lastingFor(1, MINUTES)
                                .executeSubCommand(
                                        rateLimitOutgoingTraffic().to(1, KILOBITS_PER_SECOND)
                                )
                )
                .affect(containers(containerToRateLimit.getContainerName()))
                .execute(onlyOnce().onAllChosenContainers());

        // when
        pumba.start();

        // then
        await().atMost(30, SECONDS).until(() -> {
            final PingResponse pingResponse = ping(containerToRateLimit, 117);
            assertThat(pingResponse.latencyInMilliseconds()).isGreaterThan(1000);
        });
    }
}
