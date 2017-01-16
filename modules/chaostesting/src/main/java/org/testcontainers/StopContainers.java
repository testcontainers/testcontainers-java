package org.testcontainers;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Created by novy on 17.01.17.
 */

@NoArgsConstructor(access = AccessLevel.PACKAGE)
public class StopContainers implements ContainerActions.ContainerAction {
    private long gracePeriod = 10;

    public StopContainers withSecondsToWaitBeforeKilling(long seconds) {
        this.gracePeriod = seconds;
        return this;
    }

    @Override
    public String evaluate() {
        return stopCommandPart()
                .append(gracePeriodPart())
                .evaluate();
    }

    private PumbaCommandPart stopCommandPart() {
        return () -> "stop";
    }

    private PumbaCommandPart gracePeriodPart() {
        return () -> "--time " + gracePeriod;
    }
}
