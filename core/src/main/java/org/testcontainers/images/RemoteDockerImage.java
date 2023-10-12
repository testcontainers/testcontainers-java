package org.testcontainers.images;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.PullImageCmd;
import com.github.dockerjava.api.exception.DockerClientException;
import com.google.common.util.concurrent.Futures;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.ToString;
import lombok.With;
import org.slf4j.Logger;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.ContainerFetchException;
import org.testcontainers.images.retry.ImagePullRetryPolicy;
import org.testcontainers.images.retry.PullRetryPolicy;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.DockerLoggerFactory;
import org.testcontainers.utility.ImageNameSubstitutor;
import org.testcontainers.utility.LazyFuture;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

@ToString
@AllArgsConstructor(access = AccessLevel.PACKAGE)
public class RemoteDockerImage extends LazyFuture<String> {

    @ToString.Exclude
    private Future<DockerImageName> imageNameFuture;

    @With
    ImagePullPolicy imagePullPolicy = PullPolicy.defaultPolicy();

    @With
    private ImagePullRetryPolicy imagePullRetryPolicy = PullRetryPolicy.defaultRetryPolicy();

    @With
    private ImageNameSubstitutor imageNameSubstitutor = ImageNameSubstitutor.instance();

    @ToString.Exclude
    private DockerClient dockerClient = DockerClientFactory.lazyClient();

    public RemoteDockerImage(DockerImageName dockerImageName) {
        this.imageNameFuture = CompletableFuture.completedFuture(dockerImageName);
    }

    @Deprecated
    public RemoteDockerImage(String dockerImageName) {
        this(DockerImageName.parse(dockerImageName));
    }

    @Deprecated
    public RemoteDockerImage(@NonNull String repository, @NonNull String tag) {
        this(DockerImageName.parse(repository).withTag(tag));
    }

    public RemoteDockerImage(@NonNull Future<String> imageFuture) {
        this.imageNameFuture = Futures.lazyTransform(imageFuture, DockerImageName::new);
    }

    @Override
    @SneakyThrows({ InterruptedException.class, ExecutionException.class })
    protected final String resolve() {
        final DockerImageName imageName = getImageName();
        Logger logger = DockerLoggerFactory.getLogger(imageName.toString());
        try {
            if (!imagePullPolicy.shouldPull(imageName)) {
                return imageName.asCanonicalNameString();
            }

            // The image is not available locally - pull it
            logger.info(
                "Pulling docker image: {}. Please be patient; this may take some time but only needs to be done once.",
                imageName
            );

            Exception lastFailure = null;
            boolean pull = true;
            imagePullRetryPolicy.pullStarted();

            Instant startedAt = Instant.now();

            do {
                try {
                    PullImageCmd pullImageCmd = dockerClient
                        .pullImageCmd(imageName.getUnversionedPart())
                        .withTag(imageName.getVersionPart());

                    try {
                        pullImageCmd.exec(new TimeLimitedLoggedPullImageResultCallback(logger)).awaitCompletion();
                    } catch (DockerClientException e) {
                        // Try to fallback to x86
                        pullImageCmd
                            .withPlatform("linux/amd64")
                            .exec(new TimeLimitedLoggedPullImageResultCallback(logger))
                            .awaitCompletion();
                    }
                    String dockerImageName = imageName.asCanonicalNameString();
                    logger.info("Image {} pull took {}", dockerImageName, Duration.between(startedAt, Instant.now()));

                    LocalImagesCache.INSTANCE.refreshCache(imageName);

                    return dockerImageName;
                } catch (Exception e) {
                    lastFailure = e;
                    pull = imagePullRetryPolicy.shouldRetry(imageName, e);
                }
            } while (pull);

            logger.error(
                "Failed to pull image: {}. Please check output of `docker pull {}`",
                imageName,
                imageName,
                lastFailure
            );

            throw new ContainerFetchException("Failed to pull image: " + imageName, lastFailure);
        } catch (DockerClientException e) {
            throw new ContainerFetchException("Failed to get Docker client for " + imageName, e);
        }
    }

    private DockerImageName getImageName() throws InterruptedException, ExecutionException {
        final DockerImageName specifiedImageName = imageNameFuture.get();

        // Allow the image name to be substituted
        return imageNameSubstitutor.apply(specifiedImageName);
    }

    @ToString.Include(name = "imageName", rank = 1)
    private String imageNameToString() {
        if (!imageNameFuture.isDone()) {
            return "<resolving>";
        }

        try {
            return getImageName().asCanonicalNameString();
        } catch (InterruptedException | ExecutionException e) {
            return e.getMessage();
        }
    }
}
