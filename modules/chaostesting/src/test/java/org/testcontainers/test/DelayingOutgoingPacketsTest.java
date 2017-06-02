package org.testcontainers.test;

import org.junit.Test;
import org.testcontainers.PumbaContainer;
import org.testcontainers.SupportedTimeUnit;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.test.Network.CanPingContainers;
import org.testcontainers.test.Network.PingResponse;

import static com.jayway.awaitility.Awaitility.await;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.testcontainers.NetworkActions.anAction;
import static org.testcontainers.NetworkSubCommands.delayOutgoingPackets;
import static org.testcontainers.PumbaExecutionModes.onlyOnce;
import static org.testcontainers.PumbaTargets.containers;

/**
 * Created by novy on 14.01.17.
 */
public class DelayingOutgoingPacketsTest extends ShutdownsOrphanedContainers implements CanSpawnExampleContainers, CanPingContainers {

    @Test
    public void should_be_able_to_delay_outgoing_packets_from_container() throws Exception {
        // given
        final GenericContainer aContainer = startedContainer();

        final GenericContainer<PumbaContainer> pumba = PumbaContainer.newPumba()
                .performNetworkChaos(anAction()
                        .lastingFor(1, SupportedTimeUnit.MINUTES)
                        .executeSubCommand(
                                delayOutgoingPackets()
                                        .delayFor(1000, SupportedTimeUnit.MILLISECONDS)
                        )
                )
                .affect(containers(aContainer.getContainerName()))
                .execute(onlyOnce().onAllChosenContainers());

        // when
        pumba.start();

        // then
        await().atMost(20, SECONDS).until(() -> {
            final PingResponse ping = ping(aContainer);
            assertThat(ping.latencyInMilliseconds()).isGreaterThan(900);
        });
    }
}
