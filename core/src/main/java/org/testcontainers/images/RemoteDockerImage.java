package org.testcontainers.images;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.exception.DockerClientException;
import com.github.dockerjava.api.model.Image;
import com.github.dockerjava.core.command.PullImageResultCallback;
import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.profiler.Profiler;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.ContainerFetchException;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.DockerLoggerFactory;
import org.testcontainers.utility.LazyFuture;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RemoteDockerImage extends LazyFuture<String> {

    private static final Set<String> AVAILABLE_IMAGE_NAME_CACHE = new HashSet<>();

    private final String dockerImageName;

    public RemoteDockerImage(String dockerImageName) {
        DockerImageName.validate(dockerImageName);
        this.dockerImageName = dockerImageName;
    }

    public RemoteDockerImage(@NonNull String repository, @NonNull String tag) {
        this.dockerImageName = repository + ":" + tag;
    }

    @Override
    protected final String resolve() {
        Profiler profiler = new Profiler("Rule creation - prefetch image");
        Logger logger = DockerLoggerFactory.getLogger(dockerImageName);
        profiler.setLogger(logger);

        Profiler nested = profiler.startNested("Obtaining client");
        DockerClient dockerClient = DockerClientFactory.instance().client();
        try {
            nested.stop();

            profiler.start("Check local images");

            int attempts = 0;
            while (true) {
                // Does our cache already know the image?
                if (AVAILABLE_IMAGE_NAME_CACHE.contains(dockerImageName)) {
                    logger.trace("{} is already in image name cache", dockerImageName);
                    break;
                }

                // Update the cache
                List<Image> updatedImages = dockerClient.listImagesCmd().exec();
                for (Image image : updatedImages) {
                    Collections.addAll(AVAILABLE_IMAGE_NAME_CACHE, image.getRepoTags());
                }

                // And now?
                if (AVAILABLE_IMAGE_NAME_CACHE.contains(dockerImageName)) {
                    logger.trace("{} is in image name cache following listing of images", dockerImageName);
                    break;
                }

                // Log only on first attempt
                if (attempts == 0) {
                    logger.info("Pulling docker image: {}. Please be patient; this may take some time but only needs to be done once.", dockerImageName);
                    profiler.start("Pull image");
                }

                if (attempts++ >= 3) {
                    logger.error("Retry limit reached while trying to pull image: " + dockerImageName + ". Please check output of `docker pull " + dockerImageName + "`");
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
        } catch (DockerClientException e) {
            throw new ContainerFetchException("Failed to get Docker client for " + dockerImageName, e);
        } finally {
            profiler.stop().log();
        }
    }
}
