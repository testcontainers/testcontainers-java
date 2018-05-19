package org.testcontainers.client.actions.networkactions;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
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
    public static class BernoulliModel implements NetworkSubCommands.NetworkSubCommand {
        private double percentageLoss = 0;
        private double correlation = 0;

        public BernoulliModel withLossProbability(double percentageLoss) {
            this.percentageLoss = percentageLoss;
            return this;
        }

        public BernoulliModel withCorrelation(double correlation) {
            this.correlation = correlation;
            return this;
        }

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
            return () -> "--percent " + percentageLoss;
        }

        private PumbaCommandPart correlationPart() {
            return () -> "--correlation " + correlation;
        }
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class MarkovModel implements NetworkSubCommands.NetworkSubCommand {
        private double p13Transition = 0;
        private double p31Transition = 100;
        private double p32Transition = 0;
        private double p23Transition = 100;
        private double p14Transition = 0;

        public MarkovModel withProbabilityOfTransitionFromFirstToThirdState(double probability) {
            this.p13Transition = probability;
            return this;
        }

        public MarkovModel withProbabilityOfTransitionFromThirdToFirstState(double probability) {
            this.p31Transition = probability;
            return this;
        }

        public MarkovModel withProbabilityOfTransitionFromThirdToSecondState(double probability) {
            this.p32Transition = probability;
            return this;
        }

        public MarkovModel withProbabilityOfTransitionFromSecondToThirdState(double probability) {
            this.p23Transition = probability;
            return this;
        }

        public MarkovModel withProbabilityOfTransitionFromFirstToForthState(double probability) {
            this.p14Transition = probability;
            return this;
        }

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
            return () -> "--p13 " + p13Transition;
        }

        private PumbaCommandPart p31Part() {
            return () -> "--p31 " + p31Transition;
        }

        private PumbaCommandPart p32Part() {
            return () -> "--p32 " + p32Transition;
        }

        private PumbaCommandPart p23Part() {
            return () -> "--p23 " + p23Transition;
        }

        private PumbaCommandPart p14Part() {
            return () -> "--p14 " + p14Transition;
        }
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class GilbertElliotModel implements NetworkSubCommands.NetworkSubCommand {
        private double transitionToBadStateProbability = 0;
        private double transitionToGoodStateProbability = 100;
        private double lossProbabilityInBadState = 100;
        private double lossProbabilityInGoodState = 0;

        public GilbertElliotModel withProbabilityOfTransitionToBadState(double probability) {
            this.transitionToBadStateProbability = probability;
            return this;
        }

        public GilbertElliotModel withProbabilityOfTransitionToGoodState(double probability) {
            this.transitionToGoodStateProbability = probability;
            return this;
        }

        public GilbertElliotModel withProbabilityOfPacketLossInBadState(double probability) {
            this.lossProbabilityInBadState = probability;
            return this;
        }

        public GilbertElliotModel withProbabilityOfPacketLossInGoodState(double probability) {
            this.lossProbabilityInBadState = probability;
            return this;
        }

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
            return () -> "--pb " + transitionToBadStateProbability;
        }

        private PumbaCommandPart transitionToGoodStatePart() {
            return () -> "--pg " + transitionToGoodStateProbability;
        }

        private PumbaCommandPart lossInBadStatePart() {
            return () -> "--one-h " + lossProbabilityInBadState;
        }

        private PumbaCommandPart lossInGoodStatePart() {
            return () -> "--one-k " + lossProbabilityInGoodState;
        }
    }
}
