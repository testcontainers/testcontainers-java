package org.testcontainers.test;

import org.junit.Test;
import org.testcontainers.PumbaContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.test.Network.CanPingContainers;
import org.testcontainers.test.Network.PingResponse;

import static com.jayway.awaitility.Awaitility.await;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.testcontainers.NetworkActions.anAction;
import static org.testcontainers.NetworkSubCommands.lossOutgoingPackets;
import static org.testcontainers.PumbaExecutionModes.onlyOnce;
import static org.testcontainers.PumbaTargets.containers;
import static org.testcontainers.SupportedTimeUnit.MINUTES;

/**
 * Created by novy on 17.01.17.
 */
public class LosingPacketsTest extends ShutdownsOrphanedContainers implements CanSpawnExampleContainers, CanPingContainers {

    @Test
    public void should_be_able_to_drop_outgoing_packets_with_bernoulli_model() throws Exception {
        // given
        final GenericContainer aContainer = startedContainer();

        final GenericContainer<PumbaContainer> pumba = PumbaContainer.newPumba()
                .performNetworkChaos(anAction()
                        .lastingFor(1, MINUTES)
                        .executeSubCommand(
                                lossOutgoingPackets()
                                        .accordingToBernoulliModel()
                                        .withLossProbability(100)
                        )
                )
                .affect(containers(aContainer.getContainerName()))
                .execute(onlyOnce().onAllChosenContainers());

        // when
        pumba.start();

        // then
        await().atMost(30, SECONDS).until(() -> {
            final PingResponse pingResponse = ping(aContainer);
            assertThat(pingResponse.packetLost()).isTrue();
        });
    }

    @Test
    public void should_be_able_to_drop_outgoing_packets_with_markov_model() throws Exception {
        // given
        final GenericContainer aContainer = startedContainer();

        final GenericContainer<PumbaContainer> pumba = PumbaContainer.newPumba()
                .performNetworkChaos(anAction()
                        .lastingFor(1, MINUTES)
                        .executeSubCommand(
                                lossOutgoingPackets()
                                        .accordingToMarkovModel()
                                        .withProbabilityOfTransitionFromFirstToThirdState(100)
                                        .withProbabilityOfTransitionFromThirdToFirstState(0)
                                        .withProbabilityOfTransitionFromThirdToSecondState(0)
                                        .withProbabilityOfTransitionFromSecondToThirdState(0)
                                        .withProbabilityOfTransitionFromFirstToForthState(0)
                        )
                )
                .affect(containers(aContainer.getContainerName()))
                .execute(onlyOnce().onAllChosenContainers());

        // when
        pumba.start();

        // then
        await().atMost(30, SECONDS).until(() -> {
            final PingResponse pingResponse = ping(aContainer);
            assertThat(pingResponse.packetLost()).isTrue();
        });
    }

    @Test
    public void should_be_able_to_drop_outgoing_packets_with_gilbert_elliot_model() throws Exception {
        // given
        final GenericContainer aContainer = startedContainer();

        final GenericContainer<PumbaContainer> pumba = PumbaContainer.newPumba()
                .performNetworkChaos(anAction()
                        .lastingFor(1, MINUTES)
                        .executeSubCommand(
                                lossOutgoingPackets()
                                        .accordingToGilbertElliotModel()
                                        .withProbabilityOfPacketLossInBadState(100)
                                        .withProbabilityOfTransitionToBadState(100)
                        )
                )
                .affect(containers(aContainer.getContainerName()))
                .execute(onlyOnce().onAllChosenContainers());

        // when
        pumba.start();

        // then
        await().atMost(30, SECONDS).until(() -> {
            final PingResponse pingResponse = ping(aContainer);
            assertThat(pingResponse.packetLost()).isTrue();
        });
    }
}
