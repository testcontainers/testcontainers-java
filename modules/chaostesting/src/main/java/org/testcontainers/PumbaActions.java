package org.testcontainers;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Created by novy on 31.12.16.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PumbaActions {

    public static PumbaAction killContainers() {
        // todo implement singal choosing
        return () -> "kill";
    }

    public static PumbaAction pauseContainersFor(int time, SupportedTimeUnit unit) {
        final PumbaCommandPart timePart = TimeExpression.of(time, unit);

        return () -> String.format("pause -d %s", timePart.evaluate());
    }

    public static PumbaAction stopContainers() {
        // todo implement choosing grace period
        return () -> "stop";
    }

    public static PumbaAction removeContainers() {
        // todo reimplement
        return () -> "rm --force --links --volumes";
    }

    public interface PumbaAction extends PumbaCommandPart {
    }
}
