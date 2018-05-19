package org.testcontainers.client;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.testcontainers.client.actions.PumbaAction;
import org.testcontainers.client.commandparts.PumbaCommandPart;
import org.testcontainers.client.executionmodes.PumbaExecutionMode;
import org.testcontainers.client.targets.PumbaTarget;

/**
 * Created by novy on 01.01.17.
 */

@RequiredArgsConstructor(access = AccessLevel.PACKAGE, staticName = "of")
public final class PumbaCommand {

    private final PumbaAction action;
    private final PumbaExecutionMode executionMode;
    private final PumbaTarget target;

    public String evaluate() {
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
