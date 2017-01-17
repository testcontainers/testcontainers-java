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
import static org.testcontainers.ContainerActions.dropOutgoingPackets;
import static org.testcontainers.NetworkActions.anAction;
import static org.testcontainers.PumbaExecutionModes.onlyOnce;
import static org.testcontainers.PumbaTargets.containers;

/**
 * Created by novy on 17.01.17.
 */
public class LosingPacketsTest implements CanSpawnExampleContainers, CanPingContainers {

    @Test
    public void should_be_able_to_drop_outgoing_packets() throws Exception {
        // given
        final GenericContainer aContainer = startedContainer();

        final GenericContainer<PumbaContainer> pumba = PumbaContainer.newPumba()
                .performNetworkChaos(
                        anAction()
                                .lastingFor(1, SupportedTimeUnit.MINUTES)
                                .executeSubCommand(dropOutgoingPackets().withPercentageProbability(100))
                )
                .affect(containers(aContainer.getContainerName()))
                .execute(onlyOnce().onAllChosenContainers());

        // when
        pumba.start();

        // then
        await().atMost(20, SECONDS).until(() -> {
            final PingResponse pingResponse = ping(aContainer);
            assertThat(pingResponse.wasSuccessful()).isFalse();
        });
    }
}
