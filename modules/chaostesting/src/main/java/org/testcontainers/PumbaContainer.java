package org.testcontainers;

import lombok.SneakyThrows;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.startupcheck.StartupCheckStrategy;
import org.testcontainers.images.RemoteDockerImage;
import org.testcontainers.shaded.com.github.dockerjava.api.DockerClient;

import java.util.function.Supplier;

import static org.testcontainers.containers.BindMode.READ_WRITE;

/**
 * Created by novy on 31.12.16.
 */

public final class PumbaContainer extends GenericContainer<PumbaContainer> implements PumbaDSL.ProvidesAction, PumbaDSL.ProvidesTarget, PumbaDSL.ProvidesExecutionMode {

    private Supplier<PumbaAction> action;
    private Supplier<PumbaExecutionModes.PumbaExecutionMode> executionMode;
    private Supplier<PumbaTargets.PumbaTarget> target;

    private PumbaContainer() {
        super("gaiaadm/pumba:latest");
        doNotWaitForStartupAtAll();
        mountDockerSocket();
    }

    private void doNotWaitForStartupAtAll() {
        setStartupCheckStrategy(new DoNotCheckStartup());
    }

    private void mountDockerSocket() {
        addFileSystemBind("/var/run/docker.sock", "/var/run/docker.sock", READ_WRITE);
    }

    public static PumbaDSL.ProvidesAction newPumba() {
        return new PumbaContainer();
    }

    @Override
    public PumbaDSL.ProvidesTarget performContainerChaos(ContainerActions.ContainerAction containerAction) {
        this.action = () -> containerAction;
        return this;
    }

    @Override
    public PumbaDSL.ProvidesTarget performNetworkChaos(NetworkActions.NetworkAction networkAction) {
        this.action = () -> {
            fetchIPRouteImage();
            return networkAction;
        };
        return this;
    }

    @SneakyThrows
    private void fetchIPRouteImage() {
        new RemoteDockerImage("gaiadocker/iproute2:latest").get();
    }

    @Override
    public PumbaDSL.ProvidesExecutionMode affect(PumbaTargets.PumbaTarget target) {
        this.target = () -> target;
        return this;
    }

    @Override
    public GenericContainer<PumbaContainer> execute(PumbaExecutionModes.PumbaExecutionMode executionMode) {
        this.executionMode = () -> executionMode;
        return this;
    }

    @Override
    public void start() {
        final PumbaCommand command = new PumbaCommand(action.get(), executionMode.get(), target.get());
        setCommand(command.evaluate());
        super.start();
    }

    private static class DoNotCheckStartup extends StartupCheckStrategy {
        @Override
        public StartupStatus checkStartupState(DockerClient dockerClient, String s) {
            return StartupCheckStrategy.StartupStatus.SUCCESSFUL;
        }
    }
}
