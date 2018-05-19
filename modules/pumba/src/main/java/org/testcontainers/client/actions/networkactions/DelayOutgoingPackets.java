package org.testcontainers.client.actions.networkactions;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.testcontainers.client.commandparts.PumbaCommandPart;
import org.testcontainers.client.commandparts.TimeExpression;

import java.time.Duration;
import java.util.Optional;

/**
 * Created by novy on 15.01.17.
 */
@NoArgsConstructor(access = AccessLevel.PACKAGE)
public class DelayOutgoingPackets implements NetworkSubCommands.NetworkSubCommand {
    private TimeExpression delayTime = TimeExpression.of(Duration.ofMillis(100));
    private TimeExpression jitter = TimeExpression.of(Duration.ofMillis(10));
    private int correlation = 20;
    private Distribution distribution = Distribution.NO_DISTRIBUTION;

    public DelayOutgoingPackets delayFor(Duration duration) {
        this.delayTime = TimeExpression.of(duration);
        return this;
    }

    public DelayOutgoingPackets withJitter(Duration duration) {
        this.jitter = TimeExpression.of(duration);
        return this;
    }

    public DelayOutgoingPackets withCorrelation(int correlation) {
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
