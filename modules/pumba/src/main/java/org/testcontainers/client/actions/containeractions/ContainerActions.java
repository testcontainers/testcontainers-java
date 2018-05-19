package org.testcontainers.client.actions.containeractions;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Created by novy on 31.12.16.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ContainerActions {

    public static KillContainers killContainers() {
        return new KillContainers();
    }

    public static PauseContainers pauseContainers() {
        return new PauseContainers();
    }

    public static StopContainers stopContainers() {
        return new StopContainers();
    }

    public static RemoveContainers removeContainers() {
        return new RemoveContainers();
    }

}
