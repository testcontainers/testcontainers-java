package org.testcontainers;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.testcontainers.PumbaActions.PumbaAction;
import org.testcontainers.PumbaSchedules.PumbaSchedule;
import org.testcontainers.PumbaTargets.PumbaTarget;

/**
 * Created by novy on 01.01.17.
 */

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
class PumbaCommand {

    private final PumbaAction action;
    private final PumbaSchedule schedule;
    private final PumbaTarget target;

    String evaluate() {
        return commandPrefix()
                .append(action)
                .append(schedule)
                .append(target)
                .evaluate();
    }

    private PumbaCommandPart commandPrefix() {
        // todo fixme
        return () -> "pumba --debug --host tcp://192.168.1.106:2375";
    }
}
