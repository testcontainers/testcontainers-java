package org.testcontainers;

import org.testcontainers.PumbaActions.PumbaAction;
import org.testcontainers.PumbaExecutionModes.PumbaExecutionMode;
import org.testcontainers.PumbaTargets.PumbaTarget;
import org.testcontainers.containers.GenericContainer;

/**
 * Created by novy on 31.12.16.
 */

public class PumbaContainer extends GenericContainer<PumbaContainer> {

    private PumbaAction action = PumbaActions.killContainers();
    private PumbaExecutionMode schedule = PumbaExecutionModes.onlyOnce().withAllContainersAtOnce();
    private PumbaTarget target = PumbaTargets.allContainers();

    private PumbaContainer() {
        super("gaiaadm/pumba:latest");
    }

    @Override
    public void start() {
        final PumbaCommand command = new PumbaCommand(action, schedule, target);
        setCommand(command.evaluate().replaceAll("  ", " "));
        super.start();
    }

    public static PumbaContainer newPumba() {
        return new PumbaContainer();
    }

    public PumbaContainer on(PumbaTarget target) {
        this.target = target;
        return this;
    }

    public PumbaContainer performAction(PumbaAction action) {
        this.action = action;
        return this;
    }

    public PumbaContainer schedule(PumbaExecutionMode schedule) {
        this.schedule = schedule;
        return this;
    }
}
