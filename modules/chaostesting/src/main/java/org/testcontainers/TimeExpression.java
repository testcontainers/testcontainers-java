package org.testcontainers;

import lombok.RequiredArgsConstructor;

/**
 * Created by novy on 01.01.17.
 */
@RequiredArgsConstructor(staticName = "of")
class TimeExpression implements PumbaCommandPart {

    private final int value;
    private final SupportedTimeUnit unit;

    @Override
    public String evaluate() {
        return value + unit.abbreviation();
    }
}
