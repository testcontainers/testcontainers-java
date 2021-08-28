package org.testcontainers.controller.intents;

import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.model.Frame;

public interface LogContainerIntent {
    LogContainerIntent withSince(int i);

    LogContainerIntent withFollowStream(boolean followStream);

    LogContainerIntent withStdOut(boolean withStdOut);

    LogContainerIntent withStdErr(boolean withStdErr);

    <T extends ResultCallback<Frame>> T perform(T resultCallback); // TODO: Replace param
}
