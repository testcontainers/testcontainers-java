package org.testcontainers;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

/**
 * Created by novy on 01.01.17.
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
class TimeExpression implements PumbaCommandPart {

    private final long value;
    private final SupportedTimeUnit unit;

    static TimeExpression of(int value, SupportedTimeUnit unit) {
        return new TimeExpression(value, unit);
    }

    static TimeExpression of(long value, SupportedTimeUnit unit) {
        return new TimeExpression(value, unit);
    }

    @Override
    public String evaluate() {
        return value + unit.abbreviation();
    }

    long asMilliseconds() {
        return value * unit.millisecondsMultiplier();
    }
}
