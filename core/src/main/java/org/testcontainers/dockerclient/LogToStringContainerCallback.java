package org.testcontainers.dockerclient;

import com.github.dockerjava.api.async.ResultCallbackTemplate;
import com.github.dockerjava.api.model.Frame;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LogToStringContainerCallback extends ResultCallbackTemplate<LogToStringContainerCallback, Frame> {

    private final StringBuffer logBuffer = new StringBuffer();

    @Override
    public void onNext(Frame frame) {
        log.debug(frame.toString());
        logBuffer.append(new String(frame.getPayload()));
    }

    @Override
    public String toString() {
        try {
            awaitCompletion();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return logBuffer.toString();
    }
}
