package org.testcontainers.docker.intents;

import com.github.dockerjava.api.command.TagImageCmd;
import org.testcontainers.controller.intents.TagImageIntent;

public class TagImageDockerIntent implements TagImageIntent {
    private final TagImageCmd tagImageCmd;

    public TagImageDockerIntent(TagImageCmd tagImageCmd) {
        this.tagImageCmd = tagImageCmd;
    }

    @Override
    public void perform() {
        tagImageCmd.exec();
    }
}
