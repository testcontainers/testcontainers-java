package org.testcontainers.dockerclient;

import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.model.Frame;

/**
 *
 * @deprecated use {@link ResultCallback.Adapter}
 */
@Deprecated
public class LogToStringContainerCallback extends ResultCallback.Adapter<Frame> {
    private final StringBuffer log = new StringBuffer();

    @Override
    public void onNext(Frame frame) {
        log.append(new String(frame.getPayload()));
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
