package org.testcontainers;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Optional;

/**
 * Created by novy on 14.01.17.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NetworkSubCommands {

    public static DelayOutgoingPackets delayOutgoingPackets() {
        return new DelayOutgoingPackets();
    }


    interface NetworkSubCommand extends PumbaCommandPart {
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class DelayOutgoingPackets implements NetworkSubCommand {
        private TimeExpression delayTime = TimeExpression.of(100, SupportedTimeUnit.MILLISECONDS);
        private TimeExpression jitter = TimeExpression.of(10, SupportedTimeUnit.MILLISECONDS);
        private int correlation = 20;
        private Distribution distribution = Distribution.NO_DISTRIBUTION;

        public DelayOutgoingPackets delayFor(int time, SupportedTimeUnit unit) {
            this.delayTime = TimeExpression.of(time, unit);
            return this;
        }

        public DelayOutgoingPackets withJitter(int time, SupportedTimeUnit unit) {
            this.jitter = TimeExpression.of(time, unit);
            return this;
        }

        public DelayOutgoingPackets withCorreltaion(int correlation) {
            this.correlation = correlation;
            return this;
        }

        public DelayOutgoingPackets withDistribution(Distribution distribution) {
            this.distribution = distribution;
            return this;
        }

        private PumbaCommandPart delayCommandPart() {
            return () -> "delay";
        }

        private PumbaCommandPart delayTimePart() {
            return () -> "--time " + delayTime.asMilliseconds();
        }

        private PumbaCommandPart jitterPart() {
            return () -> "--jitter " + jitter.asMilliseconds();
        }

        private PumbaCommandPart correlationPart() {
            return () -> "--correlation " + correlation;
        }

        private PumbaCommandPart distributionPart() {
            return () -> distribution.asCommandPart().map(d -> "--distribution " + d).orElse("");
        }


        @Override
        public String evaluate() {
            return delayCommandPart()
                    .append(delayTimePart())
                    .append(jitterPart())
                    .append(correlationPart())
                    .append(distributionPart())
                    .evaluate();
        }

        public enum Distribution {
            NO_DISTRIBUTION {
                @Override
                protected Optional<String> asCommandPart() {
                    return Optional.empty();
                }
            },
            UNIFORM,
            NORMAL,
            PARETO,
            PARETONORMAL;

            protected Optional<String> asCommandPart() {
                return Optional.of(name().toLowerCase());
            }
        }
    }
}
