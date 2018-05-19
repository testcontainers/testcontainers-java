package org.testcontainers.test;

import org.junit.Before;
import org.junit.Test;
import org.testcontainers.client.PumbaClient;
import org.testcontainers.client.PumbaClients;
import org.testcontainers.executables.PumbaExecutables;
import org.testcontainers.test.Pinger.PingResponse;

import java.time.Duration;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.testcontainers.client.actions.networkactions.NetworkActions.networkAction;
import static org.testcontainers.client.actions.networkactions.NetworkSubCommands.delayOutgoingPackets;
import static org.testcontainers.client.executionmodes.PumbaExecutionModes.onlyOnce;
import static org.testcontainers.client.targets.PumbaTargets.containers;

/**
 * Created by novy on 14.01.17.
 */
public class DelayingOutgoingPacketsTest implements CanSpawnContainers {

    private PumbaClient pumba;
    private Pinger pinger;

    @Before
    public void setUp() throws Exception {
        pumba = PumbaClients.forExecutable(PumbaExecutables.dockerized());
        pinger = startedPinger();
    }

    @Test
    public void should_be_able_to_delay_outgoing_packets_from_container() throws Exception {
        // given
        final Container aContainer = startedContainer();

        // when
        pumba
                .performNetworkChaos(networkAction()
                        .lastingFor(Duration.ofSeconds(30))
                        .executeSubCommand(
                                delayOutgoingPackets()
                                        .delayFor(Duration.ofMillis(500))
                        )
                )
                .affect(containers(aContainer.getContainerName()))
                .execute(onlyOnce().onAllChosenContainers());

        // then
        await().atMost(30, SECONDS).untilAsserted(() -> {
            final PingResponse ping = pinger.ping(aContainer);
            assertThat(ping.latencyInMilliseconds()).isGreaterThanOrEqualTo(450);
        });
    }
}
