package org.testcontainers.client.actions.containeractions;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.testcontainers.client.commandparts.PumbaCommandPart;
import org.testcontainers.client.commandparts.SupportedTimeUnit;
import org.testcontainers.client.commandparts.TimeExpression;

/**
 * Created by novy on 17.01.17.
 */

@NoArgsConstructor(access = AccessLevel.PACKAGE)
public class PauseContainers implements ContainerAction {
    private TimeExpression duration = TimeExpression.of(15, SupportedTimeUnit.SECONDS);

    public PauseContainers forDuration(long duration, SupportedTimeUnit unit) {
        this.duration = TimeExpression.of(duration, unit);
        return this;
    }

    @Override
    public String evaluate() {
        return pauseCommandPart()
                .append(durationPart())
                .evaluate();
    }

    private PumbaCommandPart pauseCommandPart() {
        return () -> "pause";
    }

    private PumbaCommandPart durationPart() {
        return () -> "--duration " + duration.evaluate();
    }
}
