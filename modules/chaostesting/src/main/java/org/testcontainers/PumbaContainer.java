package org.testcontainers;

import org.testcontainers.PumbaActions.PumbaAction;
import org.testcontainers.PumbaExecutionModes.PumbaExecutionMode;
import org.testcontainers.PumbaTargets.PumbaTarget;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.startupcheck.StartupCheckStrategy;
import org.testcontainers.shaded.com.github.dockerjava.api.DockerClient;

import static org.testcontainers.containers.BindMode.READ_WRITE;

/**
 * Created by novy on 31.12.16.
 */

public final class PumbaContainer extends GenericContainer<PumbaContainer> {

    private PumbaAction action;
    private PumbaExecutionMode schedule;
    private PumbaTarget target;

    private PumbaContainer() {
        super("gaiaadm/pumba:latest");
        doNotWaitForStartupAtAll();
        mountDockerSocket();

        this.action = PumbaActions.killContainers();
        this.schedule = PumbaExecutionModes.onlyOnce().withAllContainersAtOnce();
        this.target = PumbaTargets.allContainers();
    }

    private void doNotWaitForStartupAtAll() {
        setStartupCheckStrategy(new DoNotCheckStartup());
    }

    private void mountDockerSocket() {
        addFileSystemBind("/var/run/docker.sock", "/var/run/docker.sock", READ_WRITE);
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

    private static class DoNotCheckStartup extends StartupCheckStrategy {
        @Override
        public StartupStatus checkStartupState(DockerClient dockerClient, String s) {
            return StartupStatus.SUCCESSFUL;
        }
    }
}
