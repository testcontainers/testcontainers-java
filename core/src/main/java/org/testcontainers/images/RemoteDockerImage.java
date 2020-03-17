package org.testcontainers.images;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.exception.DockerClientException;
import com.github.dockerjava.api.exception.InternalServerErrorException;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.ToString;
import lombok.experimental.Wither;
import org.slf4j.Logger;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.ContainerFetchException;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.DockerLoggerFactory;
import org.testcontainers.utility.LazyFuture;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

@ToString
@AllArgsConstructor(access = AccessLevel.PACKAGE)
public class RemoteDockerImage extends LazyFuture<String> {

    private static final Duration PULL_RETRY_TIME_LIMIT = Duration.ofMinutes(2);

    @ToString.Exclude
    private Future<DockerImageName> imageNameFuture;

    @Wither
    private ImagePullPolicy imagePullPolicy = PullPolicy.defaultPolicy();

    private DockerClient dockerClient = DockerClientFactory.lazyClient();

    public RemoteDockerImage(String dockerImageName) {
        this.imageNameFuture = CompletableFuture.completedFuture(new DockerImageName(dockerImageName));
    }

    public RemoteDockerImage(@NonNull String repository, @NonNull String tag) {
        this.imageNameFuture = CompletableFuture.completedFuture(new DockerImageName(repository, tag));
    }

    public RemoteDockerImage(@NonNull Future<String> imageFuture) {
        this.imageNameFuture = new LazyFuture<DockerImageName>() {
            @Override
            @SneakyThrows({InterruptedException.class, ExecutionException.class})
            protected DockerImageName resolve() {
                return new DockerImageName(imageFuture.get());
            }
        };
    }

    @Override
    protected final String resolve() {
        final DockerImageName imageName = getImageName();
        Logger logger = DockerLoggerFactory.getLogger(imageName.toString());
        try {
            if (!imagePullPolicy.shouldPull(imageName)) {
                return imageName.toString();
            }

            // The image is not available locally - pull it
            logger.info("Pulling docker image: {}. Please be patient; this may take some time but only needs to be done once.", imageName);

            Exception lastFailure = null;
            final Instant lastRetryAllowed = Instant.now().plus(PULL_RETRY_TIME_LIMIT);

            while (Instant.now().isBefore(lastRetryAllowed)) {
                try {
                    dockerClient
                        .pullImageCmd(imageName.getUnversionedPart())
                        .withTag(imageName.getVersionPart())
                        .exec(new TimeLimitedLoggedPullImageResultCallback(logger))
                        .awaitCompletion();

                    LocalImagesCache.INSTANCE.refreshCache(imageName);

                    return imageName.toString();
                } catch (InterruptedException | InternalServerErrorException e) {
                    // these classes of exception often relate to timeout/connection errors so should be retried
                    lastFailure = e;
                    logger.warn("Retrying pull for image: {} ({}s remaining)",
                        imageName,
                        Duration.between(Instant.now(), lastRetryAllowed).getSeconds());
                }
            }
            logger.error("Failed to pull image: {}. Please check output of `docker pull {}`", imageName, imageName, lastFailure);

            throw new ContainerFetchException("Failed to pull image: " + imageName, lastFailure);
        } catch (DockerClientException e) {
            throw new ContainerFetchException("Failed to get Docker client for " + imageName, e);
        }
    }

    @ToString.Include(name = "imageName", rank = 1)
    @SneakyThrows({InterruptedException.class, ExecutionException.class})
    DockerImageName getImageName() {
       return imageNameFuture.get();
    }
}
