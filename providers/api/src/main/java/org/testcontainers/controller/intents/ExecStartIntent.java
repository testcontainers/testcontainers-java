package org.testcontainers.controller.intents;

import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.model.Frame;

public interface ExecStartIntent {
    <T extends ResultCallback<Frame>> T perform(T resultCallback);
}
