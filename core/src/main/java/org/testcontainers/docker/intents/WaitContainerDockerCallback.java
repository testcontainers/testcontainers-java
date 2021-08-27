package org.testcontainers.docker.intents;

import com.github.dockerjava.api.command.WaitContainerResultCallback;
import org.testcontainers.controller.intents.WaitContainerCallback;

public class WaitContainerDockerCallback implements WaitContainerCallback {
    private final WaitContainerResultCallback callback;

    public WaitContainerDockerCallback(WaitContainerResultCallback callback) {
        this.callback = callback;
    }

    @Override
    public Integer awaitStatusCode() {
        return callback.awaitStatusCode();
    }
}
