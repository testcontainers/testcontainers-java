package org.testcontainers.controller.intents;

import com.github.dockerjava.api.async.ResultCallback;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Map;

public interface BuildImageIntent {
    BuildImageIntent withTag(String tag);

    BuildImageIntent withDockerfilePath(String dockerFilePath);

    BuildImageIntent withDockerfile(File dockerfile);

    BuildImageIntent withPull(boolean enablePull);

    BuildImageIntent withBuildArg(String name, String value);

    BuildImageIntent withDisabledCache(boolean cacheDisabled);

    BuildImageIntent withDisabledPush(boolean disabledPush);

    @NotNull Map<String, String> getLabels();

    BuildImageIntent withLabels(Map<String, String> labels);

    <T extends ResultCallback<BuildResultItem>> T perform(T resultCallback);
}
