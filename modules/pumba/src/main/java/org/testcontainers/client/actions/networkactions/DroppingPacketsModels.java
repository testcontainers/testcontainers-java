package org.testcontainers.client.actions.networkactions;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.experimental.Wither;
import org.testcontainers.client.commandparts.PumbaCommandPart;

/**
 * Created by novy on 17.01.17.
 */

@NoArgsConstructor(access = AccessLevel.PACKAGE)
public class DroppingPacketsModels {

    public BernoulliModel accordingToBernoulliModel() {
        return new BernoulliModel();
    }

    public MarkovModel accordingToMarkovModel() {
        return new MarkovModel();
    }

    public GilbertElliotModel accordingToGilbertElliotModel() {
        return new GilbertElliotModel();
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    @Wither
    public static class BernoulliModel implements NetworkSubCommands.NetworkSubCommand {
        private double lossProbability = 0;
        private double correlation = 0;

        @Override
        public String evaluate() {
            return commandPart()
                .append(probabilityPart())
                .append(correlationPart())
                .evaluate();
        }

        private PumbaCommandPart commandPart() {
            return () -> "loss";
        }

        private PumbaCommandPart probabilityPart() {
            return () -> "--percent " + lossProbability;
        }

        private PumbaCommandPart correlationPart() {
            return () -> "--correlation " + correlation;
        }
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    @Wither
    public static class MarkovModel implements NetworkSubCommands.NetworkSubCommand {
        private double probabilityOfTransitionFromFirstToThirdState = 0;
        private double probabilityOfTransitionFromThirdToFirstState = 100;
        private double probabilityOfTransitionFromThirdToSecondState = 0;
        private double probabilityOfTransitionFromSecondToThirdState = 100;
        private double probabilityOfTransitionFromFirstToForthState = 0;

        @Override
        public String evaluate() {
            return commandPart()
                .append(p13Part())
                .append(p31Part())
                .append(p32Part())
                .append(p23Part())
                .append(p14Part())
                .evaluate();
        }

        private PumbaCommandPart commandPart() {
            return () -> "loss-state";
        }

        private PumbaCommandPart p13Part() {
            return () -> "--p13 " + probabilityOfTransitionFromFirstToThirdState;
        }

        private PumbaCommandPart p31Part() {
            return () -> "--p31 " + probabilityOfTransitionFromThirdToFirstState;
        }

        private PumbaCommandPart p32Part() {
            return () -> "--p32 " + probabilityOfTransitionFromThirdToSecondState;
        }

        private PumbaCommandPart p23Part() {
            return () -> "--p23 " + probabilityOfTransitionFromSecondToThirdState;
        }

        private PumbaCommandPart p14Part() {
            return () -> "--p14 " + probabilityOfTransitionFromFirstToForthState;
        }
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    @Wither
    public static class GilbertElliotModel implements NetworkSubCommands.NetworkSubCommand {
        private double probabilityOfTransitionToBadState = 0;
        private double probabilityOfTransitionToGoodState = 100;
        private double probabilityOfPacketLossInBadState = 100;
        private double probabilityOfPacketLossInGoodState = 0;

        @Override
        public String evaluate() {
            return commandPart()
                .append(transitionToBadStatePart())
                .append(lossInBadStatePart())
                .append(transitionToGoodStatePart())
                .append(lossInGoodStatePart())
                .evaluate();
        }

        private PumbaCommandPart commandPart() {
            return () -> "loss-gemodel";
        }

        private PumbaCommandPart transitionToBadStatePart() {
            return () -> "--pb " + probabilityOfTransitionToBadState;
        }

        private PumbaCommandPart transitionToGoodStatePart() {
            return () -> "--pg " + probabilityOfTransitionToGoodState;
        }

        private PumbaCommandPart lossInBadStatePart() {
            return () -> "--one-h " + probabilityOfPacketLossInBadState;
        }

        private PumbaCommandPart lossInGoodStatePart() {
            return () -> "--one-k " + probabilityOfPacketLossInGoodState;
        }
    }
}
