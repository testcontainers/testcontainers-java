package org.testcontainers.controller.intents;

public interface RemoveContainerIntent {
    RemoveContainerIntent withRemoveVolumes(boolean removeVolumes);

    RemoveContainerIntent withForce(boolean force);

    void perform();
}
