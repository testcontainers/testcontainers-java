package org.testcontainers.images;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.ListImagesCmd;
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

    public static final Set<String> AVAILABLE_IMAGE_NAME_CACHE = new HashSet<>();

    private final String dockerImageName;
    private final String tag;
    private final String tagSeparator;

    public RemoteDockerImage(String dockerImageName) {
        DockerImageName.validate(dockerImageName);
        final String[] splitOnSha = dockerImageName.split("@sha256:");
        if (splitOnSha.length > 1) {
            this.dockerImageName = splitOnSha[0];
            this.tag = "sha256:" + splitOnSha[1];
            this.tagSeparator = "@";
        } else {
            final int splitOnColon = dockerImageName.lastIndexOf(":");
            this.dockerImageName = dockerImageName.substring(0, splitOnColon);
            this.tag = dockerImageName.substring(splitOnColon + 1);
            this.tagSeparator = ":";
        }
    }

    public RemoteDockerImage(@NonNull String repository, @NonNull String tag) {
        this.dockerImageName = repository;
        this.tag = tag;

        if (tag.startsWith("sha256:")) {
            this.tagSeparator = "@";
        } else {
            this.tagSeparator = ":";
        }
    }

    @Override
    protected final String resolve() {
        Profiler profiler = new Profiler("Rule creation - prefetch image");
        Logger logger = DockerLoggerFactory.getLogger(concatenatedName());
        profiler.setLogger(logger);

        Profiler nested = profiler.startNested("Obtaining client");
        DockerClient dockerClient = DockerClientFactory.instance().client();
        try {
            nested.stop();

            profiler.start("Check local images");

            int attempts = 0;
            DockerClientException lastException = null;
            while (true) {
                // Does our cache already know the image?
                if (AVAILABLE_IMAGE_NAME_CACHE.contains(concatenatedName())) {
                    logger.trace("{} is already in image name cache", concatenatedName());
                    break;
                }

                // Update the cache
                ListImagesCmd listImagesCmd = dockerClient.listImagesCmd();

                if (Boolean.parseBoolean(System.getProperty("useFilter"))) {
                    listImagesCmd = listImagesCmd.withImageNameFilter(concatenatedName());
                }

                List<Image> updatedImages = listImagesCmd.exec();
                for (Image image : updatedImages) {
                    if (image.getRepoTags() != null) {
                        Collections.addAll(AVAILABLE_IMAGE_NAME_CACHE, image.getRepoTags());
                    }
                }

                // And now?
                if (AVAILABLE_IMAGE_NAME_CACHE.contains(concatenatedName())) {
                    logger.trace("{} is in image name cache following listing of images", concatenatedName());
                    break;
                }

                // Log only on first attempt
                if (attempts == 0) {
                    logger.info("Pulling docker image: {}. Please be patient; this may take some time but only needs to be done once.", concatenatedName());
                    profiler.start("Pull image");
                }

                if (attempts++ >= 3) {
                    logger.error("Retry limit reached while trying to pull image: {}. Please check output of `docker pull {}`", concatenatedName(), concatenatedName());
                    throw new ContainerFetchException("Retry limit reached while trying to pull image: " + concatenatedName(), lastException);
                }

                // The image is not available locally - pull it
                try {
                    final PullImageResultCallback callback = new PullImageResultCallback();
                    dockerClient
                        .pullImageCmd(dockerImageName)
                        .withTag(tag)
                        .exec(callback);
                    callback.awaitSuccess();
                    AVAILABLE_IMAGE_NAME_CACHE.add(concatenatedName());
                    break;
                } catch (DockerClientException e) {
                    lastException = e;
                }
            }

            return concatenatedName();
        } catch (DockerClientException e) {
            throw new ContainerFetchException("Failed to get Docker client for " + concatenatedName(), e);
        } finally {
            profiler.stop().log();
        }
    }

    private String concatenatedName() {
        return dockerImageName + tagSeparator + tag;
    }
}
