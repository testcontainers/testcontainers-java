package org.testcontainers;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.testcontainers.PumbaActions.PumbaAction;
import org.testcontainers.PumbaExecutionModes.PumbaExecutionMode;
import org.testcontainers.PumbaTargets.PumbaTarget;

/**
 * Created by novy on 01.01.17.
 */

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
class PumbaCommand {

    private final PumbaAction action;
    private final PumbaExecutionMode executionMode;
    private final PumbaTarget target;

    String evaluate() {
        return commandPrefix()
                .append(executionMode)
                .append(action)
                .append(target)
                .evaluate();
    }

    private PumbaCommandPart commandPrefix() {
        // todo fixme
        return () -> "pumba --debug --host tcp://192.168.1.106:2375";
    }
}
