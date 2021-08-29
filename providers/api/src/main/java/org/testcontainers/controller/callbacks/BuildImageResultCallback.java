package org.testcontainers.controller.callbacks;

import com.github.dockerjava.api.async.ResultCallbackTemplate;
import com.github.dockerjava.api.exception.DockerClientException;
import lombok.extern.slf4j.Slf4j;
import org.testcontainers.controller.intents.BuildResultItem;

import java.util.concurrent.TimeUnit;

@Slf4j
public class BuildImageResultCallback extends ResultCallbackTemplate<BuildImageResultCallback, BuildResultItem> {
    private String imageId;

    private String error;

    @Override
    public void onNext(BuildResultItem item) {
        if (item.isBuildSuccessIndicated()) {
            this.imageId = item.getImageId();
        } else if (item.isErrorIndicated()) {
            this.error = item.getError();
        }
        log.debug(item.toString());
    }

    /**
     * Awaits the image id from the response stream.
     *
     * @throws DockerClientException
     *             if the build fails.
     */
    public String awaitImageId() {
        try {
            awaitCompletion();
        } catch (InterruptedException e) {
            throw new DockerClientException("", e);
        }

        return getImageId();
    }

    /**
     * Awaits the image id from the response stream.
     *
     * @throws DockerClientException
     *             if the build fails or the timeout occurs.
     */
    public String awaitImageId(long timeout, TimeUnit timeUnit) {
        try {
            awaitCompletion(timeout, timeUnit);
        } catch (InterruptedException e) {
            throw new DockerClientException("Awaiting image id interrupted: ", e);
        }

        return getImageId();
    }

    private String getImageId() {
        if (imageId != null) {
            return imageId;
        }

        if (error == null) {
            throw new DockerClientException("Could not build image");
        }

        throw new DockerClientException("Could not build image: " + error);
    }
}
