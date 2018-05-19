package org.testcontainers.test;

import org.junit.Before;
import org.junit.Test;
import org.testcontainers.client.PumbaClient;
import org.testcontainers.client.PumbaClients;
import org.testcontainers.client.commandparts.SupportedTimeUnit;
import org.testcontainers.executables.PumbaExecutables;
import org.testcontainers.test.Pinger.PingResponse;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.testcontainers.client.actions.networkactions.NetworkActions.networkAction;
import static org.testcontainers.client.actions.networkactions.NetworkSubCommands.rateLimitOutgoingTraffic;
import static org.testcontainers.client.actions.networkactions.RateLimitOutgoingTraffic.RateUnit.KILOBITS_PER_SECOND;
import static org.testcontainers.client.executionmodes.PumbaExecutionModes.onlyOnce;
import static org.testcontainers.client.targets.PumbaTargets.containers;

/**
 * Created by novy on 15.01.17.
 */
public class RateLimitingOutgoingPacketsTest implements CanSpawnContainers {

    private PumbaClient pumba;
    private Pinger pinger;

    @Before
    public void setUp() throws Exception {
        pumba = PumbaClients.forExecutable(PumbaExecutables.dockerized());
        pinger = startedPinger();
    }

    @Test
    public void should_be_able_to_rate_limit_outgoing_packets_from_container() throws Exception {
        // given
        final Container containerToRateLimit = startedContainer();

        // when
        pumba
                .performNetworkChaos(
                        networkAction()
                                .lastingFor(30, SupportedTimeUnit.SECONDS)
                                .executeSubCommand(
                                        rateLimitOutgoingTraffic().to(1, KILOBITS_PER_SECOND)
                                )
                )
                .affect(containers(containerToRateLimit.getContainerName()))
                .execute(onlyOnce().onAllChosenContainers());

        // then
        await().atMost(30, SECONDS).untilAsserted(() -> {
            final PingResponse pingResponse = pinger.ping(containerToRateLimit, 117);
            assertThat(pingResponse.latencyInMilliseconds()).isGreaterThan(1000);
        });
    }
}
