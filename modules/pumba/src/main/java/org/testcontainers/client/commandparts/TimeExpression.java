package org.testcontainers.client.commandparts;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

/**
 * Created by novy on 01.01.17.
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class TimeExpression implements PumbaCommandPart {

    private final long value;
    private final SupportedTimeUnit unit;

    public static TimeExpression of(int value, SupportedTimeUnit unit) {
        return new TimeExpression(value, unit);
    }

    public static TimeExpression of(long value, SupportedTimeUnit unit) {
        return new TimeExpression(value, unit);
    }

    @Override
    public String evaluate() {
        return value + unit.abbreviation();
    }

    public long asMilliseconds() {
        return value * unit.millisecondsMultiplier();
    }
}
