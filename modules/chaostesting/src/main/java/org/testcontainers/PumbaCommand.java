package org.testcontainers;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

/**
 * Created by novy on 01.01.17.
 */

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
class PumbaCommand {

    private final PumbaAction action;
    private final PumbaExecutionModes.PumbaExecutionMode executionMode;
    private final PumbaTargets.PumbaTarget target;

    String evaluate() {
        return commandPrefix()
                .append(executionMode)
                .append(action)
                .append(target)
                .evaluate();
    }

    private PumbaCommandPart commandPrefix() {
        return () -> "pumba --debug";
    }
}
