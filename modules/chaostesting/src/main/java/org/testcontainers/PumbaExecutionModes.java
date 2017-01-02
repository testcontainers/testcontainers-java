package org.testcontainers;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

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

        public PumbaExecutionMode withAllContainersAtOnce() {
            return new PumbaExecutionMode(schedule, () -> "");
        }

        public PumbaExecutionMode withOneContainerAtTime() {
            return new PumbaExecutionMode(schedule, () -> "--random");
        }
    }

    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    public static final class PumbaExecutionMode implements PumbaCommandPart {
        private final PumbaCommandPart schedulePart;
        private final PumbaCommandPart containersToAffect;

        @Override
        public String evaluate() {
            return schedulePart.append(containersToAffect).evaluate();
        }
    }
}
