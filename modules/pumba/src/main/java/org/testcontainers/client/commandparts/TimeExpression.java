package org.testcontainers.client.commandparts;

import lombok.RequiredArgsConstructor;

import java.time.Duration;

/**
 * Created by novy on 01.01.17.
 */
@RequiredArgsConstructor(staticName = "of")
public class TimeExpression implements PumbaCommandPart {

    private final Duration duration;

    @Override
    public String evaluate() {
        return asMilliseconds() + "ms";
    }

    public long asMilliseconds() {
        return duration.toMillis();
    }
}
