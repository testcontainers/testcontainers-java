package org.testcontainers.docker.intents;

import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.model.BuildResponseItem;
import org.testcontainers.controller.intents.BuildResultItem;
import org.testcontainers.docker.model.DockerBuildResponseItem;

import java.io.Closeable;
import java.io.IOException;

// TODO: Move to other package?
public class BuildImageDockerCallback<T extends ResultCallback<BuildResultItem>> implements ResultCallback<BuildResponseItem>{
    private final T resultCallback;

    public BuildImageDockerCallback(T resultCallback) {
        this.resultCallback = resultCallback;
    }

    @Override
    public void onStart(Closeable closeable) {
        resultCallback.onStart(closeable);
    }

    @Override
    public void onNext(BuildResponseItem item) {
        resultCallback.onNext(new DockerBuildResponseItem(item));
    }

    @Override
    public void onError(Throwable throwable) {
        this.resultCallback.onError(throwable);
    }

    @Override
    public void onComplete() {
        this.resultCallback.onComplete();
    }

    @Override
    public void close() throws IOException {
        this.resultCallback.close();
    }
}
