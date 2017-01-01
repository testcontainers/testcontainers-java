package org.testcontainers;

import org.testcontainers.PumbaActions.PumbaAction;
import org.testcontainers.PumbaSchedules.PumbaSchedule;
import org.testcontainers.PumbaTargets.PumbaTarget;
import org.testcontainers.containers.GenericContainer;

/**
 * Created by novy on 31.12.16.
 */

public class PumbaContainer extends GenericContainer<PumbaContainer> {

    private PumbaAction action = PumbaActions.killContainers();
    private PumbaSchedule schedule = PumbaSchedules.onlyOnce();
    private PumbaTarget target = PumbaTargets.allContainers();

    private PumbaContainer() {
        super("gaiaadm/pumba:latest");
    }

    @Override
    public void start() {
        final PumbaCommand command = new PumbaCommand(action, schedule, target);
        setCommand(command.evaluate());
        super.start();
    }

    public static PumbaContainer newPumba() {
        return new PumbaContainer();
    }

    public PumbaContainer affectingContainers(PumbaTarget target) {
        this.target = target;
        return this;
    }

    public PumbaContainer performingAction(PumbaAction action) {
        this.action = action;
        return this;
    }

    public PumbaContainer scheduled(PumbaSchedule schedule) {
        this.schedule = schedule;
        return this;
    }
}
