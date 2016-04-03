package org.testcontainers.containers;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Image;
import com.github.dockerjava.core.command.PullImageResultCallback;
import com.google.common.util.concurrent.Futures;
import lombok.NonNull;
import lombok.experimental.Delegate;
import org.slf4j.LoggerFactory;
import org.slf4j.profiler.Profiler;
import org.testcontainers.DockerClientFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

public class RemoteDockerImage implements Future<String> {

    private static final Set<String> AVAILABLE_IMAGE_NAME_CACHE = new HashSet<>();

    @Delegate
    private final Future<String> delegate;

    public RemoteDockerImage(final @NonNull String providedImageName) {
        delegate = Futures.lazyTransform(CompletableFuture.completedFuture(providedImageName), dockerImageName -> {

            Profiler profiler = new Profiler("Rule creation - prefetch image");

            if ("UTF-8".equals(System.getProperty("file.encoding"))) {
                profiler.setLogger(LoggerFactory.getLogger("\uD83D\uDC33 [" + dockerImageName + "]"));
            } else {
                profiler.setLogger(LoggerFactory.getLogger("docker[" + dockerImageName + "]"));
            }

            try (DockerClient dockerClient = DockerClientFactory.instance().client()) {

                profiler.start("Check local images");

                int attempts = 0;
                while (true) {
                    // Does our cache already know the image?
                    if (AVAILABLE_IMAGE_NAME_CACHE.contains(dockerImageName)) {
                        break;
                    }

                    // Update the cache
                    List<Image> updatedImages = dockerClient.listImagesCmd().exec();
                    for (Image image : updatedImages) {
                        Collections.addAll(AVAILABLE_IMAGE_NAME_CACHE, image.getRepoTags());
                    }

                    // And now?
                    if (AVAILABLE_IMAGE_NAME_CACHE.contains(dockerImageName)) {
                        break;
                    }

                    // Log only on first attempt
                    if (attempts == 0) {
                        profiler.getLogger().info("Pulling docker image: {}. Please be patient; this may take some time but only needs to be done once.", dockerImageName);
                        profiler.start("Pull image");
                    }

                    if (attempts++ >= 3) {
                        profiler.getLogger().error("Retry limit reached while trying to pull image: " + dockerImageName + ". Please check output of `docker pull " + dockerImageName + "`");
                        throw new ContainerFetchException("Retry limit reached while trying to pull image: " + dockerImageName);
                    }

                    // The image is not available locally - pull it
                    try {
                        dockerClient.pullImageCmd(dockerImageName).exec(new PullImageResultCallback()).awaitCompletion();
                    } catch (InterruptedException e) {
                        throw new ContainerFetchException("Failed to fetch container image for " + dockerImageName, e);
                    }

                    // Do not break here, but step into the next iteration, where it will be verified with listImagesCmd().
                    // see https://github.com/docker/docker/issues/10708
                }

                return dockerImageName;
            } catch (IOException e) {
                throw new ContainerFetchException("Failed to get Docker client for " + dockerImageName, e);
            } finally {
                profiler.stop().log();
            }
        });
    }

}
