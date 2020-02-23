package org.testcontainers.dockerclient;

import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.core.command.LogContainerResultCallback;

public class LogToStringContainerCallback extends LogContainerResultCallback {
    private final StringBuffer log = new StringBuffer();

    @Override
    public void onNext(Frame frame) {
        log.append(new String(frame.getPayload()));
        super.onNext(frame);
    }

    @Override
    public String toString() {
        try {
            awaitCompletion();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return log.toString();
    }
}
