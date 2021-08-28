package org.testcontainers.controller.intents;

import com.github.dockerjava.api.command.DisconnectFromNetworkCmd;

public interface RemoveContainerIntent {
    RemoveContainerIntent withRemoveVolumes(boolean removeVolumes);

    RemoveContainerIntent withForce(boolean force);

    void perform();
}
