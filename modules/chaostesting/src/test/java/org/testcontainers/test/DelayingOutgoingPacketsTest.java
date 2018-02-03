package org.testcontainers.test;

import org.junit.Before;
import org.junit.Test;
import org.testcontainers.PumbaExecutables;
import org.testcontainers.client.PumbaClient;
import org.testcontainers.client.commandparts.SupportedTimeUnit;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.test.Network.CanPingContainers;
import org.testcontainers.test.Network.PingResponse;

import static com.jayway.awaitility.Awaitility.await;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.testcontainers.client.actions.networkactions.NetworkActions.networkAction;
import static org.testcontainers.client.actions.networkactions.NetworkSubCommands.delayOutgoingPackets;
import static org.testcontainers.client.executionmodes.PumbaExecutionModes.onlyOnce;
import static org.testcontainers.client.targets.PumbaTargets.containers;

/**
 * Created by novy on 14.01.17.
 */
public class DelayingOutgoingPacketsTest implements CanSpawnExampleContainers, CanPingContainers {

    private PumbaClient pumba;

    @Before
    public void setUp() throws Exception {
        pumba = new PumbaClient(PumbaExecutables.dockerized());
    }

    @Test
    public void should_be_able_to_delay_outgoing_packets_from_container() throws Exception {
        // given
        final GenericContainer aContainer = startedContainer();

        // when
        pumba
                .performNetworkChaos(networkAction()
                        .lastingFor(1, SupportedTimeUnit.MINUTES)
                        .executeSubCommand(
                                delayOutgoingPackets()
                                        .delayFor(1000, SupportedTimeUnit.MILLISECONDS)
                        )
                )
                .affect(containers(aContainer.getContainerName()))
                .execute(onlyOnce().onAllChosenContainers());

        // then
        await().atMost(20, SECONDS).until(() -> {
            final PingResponse ping = ping(aContainer);
            assertThat(ping.latencyInMilliseconds()).isGreaterThan(900);
        });
    }
}
