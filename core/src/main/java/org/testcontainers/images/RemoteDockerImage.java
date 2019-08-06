package org.testcontainers.images;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.ListImagesCmd;
import com.github.dockerjava.api.exception.DockerClientException;
import com.github.dockerjava.api.exception.InternalServerErrorException;
import com.github.dockerjava.api.model.Image;
import com.github.dockerjava.core.command.PullImageResultCallback;
import lombok.NonNull;
import lombok.ToString;
import org.slf4j.Logger;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.ContainerFetchException;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.DockerLoggerFactory;
import org.testcontainers.utility.LazyFuture;

import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.time.Duration.between;
import static java.time.Instant.now;

@ToString
public class RemoteDockerImage extends LazyFuture<String> {

    /**
     * @deprecated this field will become private in a later release
     */
    @Deprecated
    public static final Set<DockerImageName> AVAILABLE_IMAGE_NAME_CACHE = new HashSet<>();
    private static final Duration PULL_RETRY_TIME_LIMIT = Duration.ofMinutes(2);

    private DockerImageName imageName;

    public RemoteDockerImage(String dockerImageName) {
        imageName = new DockerImageName(dockerImageName);
    }

    public RemoteDockerImage(@NonNull String repository, @NonNull String tag) {
        imageName = new DockerImageName(repository, tag);
    }

    @Override
    protected final String resolve() {
        Logger logger = DockerLoggerFactory.getLogger(imageName.toString());

        DockerClient dockerClient = DockerClientFactory.instance().client();
        try {
            // Does our cache already know the image?
            if (AVAILABLE_IMAGE_NAME_CACHE.contains(imageName)) {
                logger.trace("{} is already in image name cache", imageName);
                return imageName.toString();
            }

            // Update the cache
            ListImagesCmd listImagesCmd = dockerClient.listImagesCmd();

            if (Boolean.parseBoolean(System.getProperty("useFilter"))) {
                listImagesCmd = listImagesCmd.withImageNameFilter(imageName.toString());
            }

            List<Image> updatedImages = listImagesCmd.exec();
            updatedImages.stream()
                .map(Image::getRepoTags)
                .filter(Objects::nonNull)
                .flatMap(Stream::of)
                .map(DockerImageName::new)
                .collect(Collectors.toCollection(() -> AVAILABLE_IMAGE_NAME_CACHE));

            // And now?
            if (AVAILABLE_IMAGE_NAME_CACHE.contains(imageName)) {
                logger.trace("{} is in image name cache following listing of images", imageName);
                return imageName.toString();
            }

            // The image is not available locally - pull it
            logger.info("Pulling docker image: {}. Please be patient; this may take some time but only needs to be done once.", imageName);

            Exception lastFailure = null;
            final Instant lastRetryAllowed = now().plus(PULL_RETRY_TIME_LIMIT);

            while (now().isBefore(lastRetryAllowed)) {
                try {
                    final PullImageResultCallback callback = new TimeLimitedLoggedPullImageResultCallback(logger);
                    dockerClient
                        .pullImageCmd(imageName.getUnversionedPart())
                        .withTag(imageName.getVersionPart())
                        .exec(callback);
                    callback.awaitCompletion();
                    AVAILABLE_IMAGE_NAME_CACHE.add(imageName);

                    return imageName.toString();
                } catch (InterruptedException | InternalServerErrorException e) {
                    // these classes of exception often relate to timeout/connection errors so should be retried
                    lastFailure = e;
                    logger.warn("Retrying pull for image: {} ({}s remaining)",
                        imageName,
                        between(now(), lastRetryAllowed).getSeconds());
                }
            }
            logger.error("Failed to pull image: {}. Please check output of `docker pull {}`", imageName, imageName, lastFailure);

            throw new ContainerFetchException("Failed to pull image: " + imageName, lastFailure);
        } catch (DockerClientException e) {
            throw new ContainerFetchException("Failed to get Docker client for " + imageName, e);
        }
    }
}
