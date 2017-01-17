package org.testcontainers;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Created by novy on 17.01.17.
 */

public class DroppingPackets {

    @NoArgsConstructor(access = AccessLevel.PACKAGE)
    public static class BernoulliModel implements NetworkSubCommands.NetworkSubCommand {
        private double percentageLoss = 0;
        private double correlation = 0;

        public BernoulliModel withPercentageProbability(double percentageLoss) {
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
}
