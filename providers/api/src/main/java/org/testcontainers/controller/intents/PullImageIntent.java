package org.testcontainers.controller.intents;

import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.model.PullResponseItem;

public interface PullImageIntent {
    PullImageIntent withTag(String tag);

    PullImageIntent withPlatform(String platform);

    <T extends ResultCallback<PullResponseItem>> T perform(T resultCallback);
}
