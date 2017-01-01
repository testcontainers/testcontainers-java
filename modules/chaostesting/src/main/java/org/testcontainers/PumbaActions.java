package org.testcontainers;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Created by novy on 31.12.16.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PumbaActions {

    public static PumbaAction killContainers() {
        return () -> "kill";
    }

    public static PumbaAction pauseContainersFor(int time, SupportedTimeUnit unit) {
        final PumbaCommandPart timePart = TimeExpression.of(time, unit);

        return () -> String.format("pause -d %s", timePart.evaluate());
    }

    public interface PumbaAction extends PumbaCommandPart {
    }
}
