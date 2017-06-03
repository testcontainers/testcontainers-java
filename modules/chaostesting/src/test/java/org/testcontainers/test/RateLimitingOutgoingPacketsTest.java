package org.testcontainers.test;

import org.junit.Before;
import org.junit.Test;
import org.testcontainers.PumbaExecutables;
import org.testcontainers.client.PumbaClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.test.Network.CanPingContainers;
import org.testcontainers.test.Network.PingResponse;

import static com.jayway.awaitility.Awaitility.await;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.testcontainers.client.actions.networkactions.NetworkActions.anAction;
import static org.testcontainers.client.actions.networkactions.NetworkSubCommands.rateLimitOutgoingTraffic;
import static org.testcontainers.client.executionmodes.PumbaExecutionModes.onlyOnce;
import static org.testcontainers.client.targets.PumbaTargets.containers;
import static org.testcontainers.client.actions.networkactions.RateLimitOutgoingTraffic.RateUnit.KILOBITS_PER_SECOND;
import static org.testcontainers.client.commandparts.SupportedTimeUnit.MINUTES;

/**
 * Created by novy on 15.01.17.
 */
public class RateLimitingOutgoingPacketsTest implements CanSpawnExampleContainers, CanPingContainers {

    private PumbaClient pumba;

    @Before
    public void setUp() throws Exception {
        pumba = new PumbaClient(PumbaExecutables.dockerized());
    }

    @Test
    public void should_be_able_to_rate_limit_outgoing_packets_from_container() throws Exception {
        // given
        final GenericContainer containerToRateLimit = startedContainer();

        // when
        pumba
                .performNetworkChaos(
                        anAction()
                                .lastingFor(1, MINUTES)
                                .executeSubCommand(
                                        rateLimitOutgoingTraffic().to(1, KILOBITS_PER_SECOND)
                                )
                )
                .affect(containers(containerToRateLimit.getContainerName()))
                .execute(onlyOnce().onAllChosenContainers());

        // then
        await().atMost(30, SECONDS).until(() -> {
            final PingResponse pingResponse = ping(containerToRateLimit, 117);
            assertThat(pingResponse.latencyInMilliseconds()).isGreaterThan(1000);
        });
    }
}
