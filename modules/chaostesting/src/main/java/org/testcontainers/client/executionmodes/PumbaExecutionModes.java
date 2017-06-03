package org.testcontainers.client.executionmodes;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.testcontainers.client.commandparts.PumbaCommandPart;
import org.testcontainers.client.commandparts.SupportedTimeUnit;
import org.testcontainers.client.commandparts.TimeExpression;

/**
 * Created by novy on 01.01.17.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PumbaExecutionModes {

    public static WithSchedule onlyOnce() {
        return new WithSchedule(
                () -> ""
        );
    }

    public static WithSchedule recurrently(int time, SupportedTimeUnit unit) {
        return new WithSchedule(
                () -> "--interval " + TimeExpression.of(time, unit).evaluate()
        );
    }

    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    public static class WithSchedule {
        private final PumbaCommandPart schedule;

        public PumbaExecutionMode onAllChosenContainers() {
            return new PumbaExecutionMode(schedule, () -> "");
        }

        public PumbaExecutionMode onRandomlyChosenContainer() {
            return new PumbaExecutionMode(schedule, () -> "--random");
        }
    }

}
