package org.testcontainers.client;


import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.testcontainers.client.actions.containeractions.ContainerAction;
import org.testcontainers.client.actions.networkactions.NetworkAction;
import org.testcontainers.client.executionmodes.PumbaExecutionMode;
import org.testcontainers.client.targets.PumbaTarget;

/**
 * Created by novy on 14.01.17.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PumbaDSL {

    public interface ChooseAction {
        ChooseTarget performContainerChaos(ContainerAction containerAction);

        ChooseTarget performNetworkChaos(NetworkAction networkAction);
    }

    public interface ChooseTarget {
        ChooseExecutionMode affect(PumbaTarget target);
    }

    public interface ChooseExecutionMode {
        void execute(PumbaExecutionMode executionMode);
    }
}
