package org.testcontainers.docker.intents;

import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.PullImageCmd;
import com.github.dockerjava.api.model.PullResponseItem;
import org.testcontainers.controller.intents.PullImageIntent;

public class PullImageDockerIntent implements PullImageIntent {
    private final PullImageCmd pullImageCmd;

    public PullImageDockerIntent(PullImageCmd pullImageCmd) {
        this.pullImageCmd = pullImageCmd;
    }

    @Override
    public PullImageIntent withTag(String tag) {
        pullImageCmd.withTag(tag);
        return this;
    }

    @Override
    public PullImageIntent withPlatform(String platform) {
        pullImageCmd.withPlatform(platform);
        return this;
    }

    @Override
    public <T extends ResultCallback<PullResponseItem>> T perform(T resultCallback) {
        return pullImageCmd.exec(resultCallback);
    }
}
