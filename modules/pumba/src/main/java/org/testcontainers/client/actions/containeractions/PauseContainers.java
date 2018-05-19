package org.testcontainers.client.actions.containeractions;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.testcontainers.client.commandparts.PumbaCommandPart;
import org.testcontainers.client.commandparts.TimeExpression;

import java.time.Duration;

/**
 * Created by novy on 17.01.17.
 */

@NoArgsConstructor(access = AccessLevel.PACKAGE)
public class PauseContainers implements ContainerAction {
    private TimeExpression duration = TimeExpression.of(Duration.ofSeconds(15));

    public PauseContainers forDuration(Duration duration) {
        this.duration = TimeExpression.of(duration);
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
