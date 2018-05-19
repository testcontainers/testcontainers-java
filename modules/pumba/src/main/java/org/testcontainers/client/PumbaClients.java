package org.testcontainers.client;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.testcontainers.client.actions.containeractions.ContainerAction;
import org.testcontainers.client.actions.networkactions.NetworkAction;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PumbaClients {

    public static PumbaClient forExecutable(PumbaExecutable executable) {
        final InternalPumbaClient internalClient = new InternalPumbaClient(executable);

        return new PumbaClient() {
            @Override
            public PumbaDSL.ChooseTarget performContainerChaos(ContainerAction containerAction) {
                return internalClient.performContainerChaos(containerAction);
            }

            @Override
            public PumbaDSL.ChooseTarget performNetworkChaos(NetworkAction networkAction) {
                return internalClient.performNetworkChaos(networkAction);
            }
        };
    }
}
