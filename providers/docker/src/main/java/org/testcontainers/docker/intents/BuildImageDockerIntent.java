package org.testcontainers.docker.intents;

import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.BuildImageCmd;
import org.jetbrains.annotations.NotNull;
import org.testcontainers.controller.intents.BuildImageIntent;
import org.testcontainers.controller.intents.BuildResultItem;

import java.io.File;
import java.util.Collections;
import java.util.Map;

public class BuildImageDockerIntent implements BuildImageIntent {
    private final BuildImageCmd buildImageCmd;

    public BuildImageDockerIntent(BuildImageCmd buildImageCmd) {
        this.buildImageCmd = buildImageCmd;
    }

    @Override
    public BuildImageIntent withTag(String tag) {
        buildImageCmd.withTag(tag);
        return this;
    }

    @Override
    public BuildImageIntent withDockerfilePath(String dockerFilePath) {
        buildImageCmd.withDockerfilePath(dockerFilePath);
        return this;
    }

    @Override
    public BuildImageIntent withDockerfile(File dockerfile) {
        buildImageCmd.withDockerfile(dockerfile);
        return this;
    }

    @Override
    public BuildImageIntent withPull(boolean enablePull) {
        buildImageCmd.withPull(enablePull);
        return this;
    }

    @Override
    public BuildImageIntent withBuildArg(String name, String value) {
        buildImageCmd.withBuildArg(name, value);
        return this;
    }

    @Override
    public BuildImageIntent withDisabledCache(boolean disabledCache) {
        buildImageCmd.withNoCache(disabledCache);
        return this;
    }

    @Override
    public BuildImageIntent withDisabledPush(boolean disabledPush) {
        throw new RuntimeException("Not yet implemented!"); // TODO: Implement!
    }

    @Override
    public @NotNull Map<String, String> getLabels() {
        Map<String, String> labels = buildImageCmd.getLabels();
        if(labels == null) {
            return Collections.emptyMap();
        }
        return labels;
    }

    @Override
    public BuildImageIntent withLabels(Map<String, String> labels) {
        buildImageCmd.withLabels(labels);
        return this;
    }

    @Override
    public <T extends ResultCallback<BuildResultItem>> T perform(T resultCallback) {
        buildImageCmd.exec(new BuildImageDockerCallback(resultCallback));
        return resultCallback;
    }

//    @Override
//    public <T extends ResultCallback<BuildResponseItem>> T perform(T resultCallback) {
//        return buildImageCmd.exec(resultCallback);
//    }
}
