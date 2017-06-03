package org.testcontainers.client;

import lombok.RequiredArgsConstructor;
import org.testcontainers.client.actions.PumbaAction;
import org.testcontainers.client.actions.containeractions.ContainerAction;
import org.testcontainers.client.actions.networkactions.NetworkAction;
import org.testcontainers.client.executionmodes.PumbaExecutionMode;
import org.testcontainers.client.targets.PumbaTarget;

/**
 * Created by novy on 03.06.17.
 */
@RequiredArgsConstructor
public final class PumbaClient implements PumbaDSL.ChooseAction, PumbaDSL.ChooseTarget, PumbaDSL.ChooseExecutionMode {

    private final PumbaExecutable executable;

    private PumbaAction action;
    private PumbaExecutionMode executionMode;
    private PumbaTarget target;

    @Override
    public PumbaDSL.ChooseTarget performContainerChaos(ContainerAction containerAction) {
        this.action = containerAction;
        return this;
    }

    @Override
    public PumbaDSL.ChooseTarget performNetworkChaos(NetworkAction networkAction) {
        this.action = networkAction;
        return this;
    }

    @Override
    public PumbaDSL.ChooseExecutionMode affect(PumbaTarget target) {
        this.target = target;
        return this;
    }

    @Override
    public void execute(PumbaExecutionMode executionMode) {
        this.executionMode = executionMode;
        buildAndExecuteCommand();
    }

    private void buildAndExecuteCommand() {
        PumbaCommand commandToExecute = PumbaCommand.of(action, executionMode, target);
        executable.execute(commandToExecute);
    }
}
