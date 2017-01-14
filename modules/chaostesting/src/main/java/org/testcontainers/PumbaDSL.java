package org.testcontainers;


import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.testcontainers.containers.GenericContainer;

/**
 * Created by novy on 14.01.17.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
class PumbaDSL {

    public interface ProvidesAction {
        ProvidesTarget performContainerChaos(ContainerActions.ContainerAction containerAction);

        ProvidesTarget performNetworkChaos(NetworkActions.NetworkAction networkAction);
    }

    public interface ProvidesTarget {
        ProvidesExecutionMode affect(PumbaTargets.PumbaTarget target);
    }

    public interface ProvidesExecutionMode {
        GenericContainer<PumbaContainer> execute(PumbaExecutionModes.PumbaExecutionMode executionMode);
    }
}
