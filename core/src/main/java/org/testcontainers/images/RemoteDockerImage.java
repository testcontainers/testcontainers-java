package org.testcontainers.images;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.PullImageCmd;
import com.github.dockerjava.api.exception.DockerClientException;
import com.github.dockerjava.api.exception.InternalServerErrorException;
import com.google.common.util.concurrent.Futures;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.ToString;
import lombok.With;
import org.awaitility.Awaitility;
import org.awaitility.pollinterval.IterativePollInterval;
import org.awaitility.pollinterval.PollInterval;
import org.slf4j.Logger;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.ContainerFetchException;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.DockerLoggerFactory;
import org.testcontainers.utility.ImageNameSubstitutor;
import org.testcontainers.utility.LazyFuture;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

@ToString
@AllArgsConstructor(access = AccessLevel.PACKAGE)
public class RemoteDockerImage extends LazyFuture<String> {

    private static final Duration PULL_RETRY_TIME_LIMIT = Duration.ofMinutes(2);

    @ToString.Exclude
    private Future<DockerImageName> imageNameFuture;

    @With
    ImagePullPolicy imagePullPolicy = PullPolicy.defaultPolicy();

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
        final Logger logger = DockerLoggerFactory.getLogger(imageName.toString());
        try {
            if (!imagePullPolicy.shouldPull(imageName)) {
                return imageName.asCanonicalNameString();
            }

            // The image is not available locally - pull it
            logger.info(
                "Pulling docker image: {}. Please be patient; this may take some time but only needs to be done once.",
                imageName
            );

            final Instant startedAt = Instant.now();
            final Instant lastRetryAllowed = Instant.now().plus(PULL_RETRY_TIME_LIMIT);
            final AtomicReference<Exception> lastFailure = new AtomicReference<>();
            final PullImageCmd pullImageCmd = dockerClient
                .pullImageCmd(imageName.getUnversionedPart())
                .withTag(imageName.getVersionPart());
            final AtomicReference<String> dockerImageName = new AtomicReference<>();

            // The following poll interval in ms: 50, 100, 200, 400, 800....
            // Results in ~70 requests in over 2 minutes
            final PollInterval interval = IterativePollInterval
                .iterative(duration -> duration.multipliedBy(2))
                .startDuration(Duration.ofMillis(50));

            Awaitility
                .await()
                .pollInSameThread()
                .pollDelay(Duration.ZERO) // start checking immediately
                .atMost(PULL_RETRY_TIME_LIMIT)
                .pollInterval(interval)
                .until(
                    tryImagePullCommand(pullImageCmd, logger, dockerImageName, imageName, lastFailure, lastRetryAllowed)
                );

            if (dockerImageName.get() == null) {
                final Exception lastException = lastFailure.get();
                logger.error(
                    "Failed to pull image: {}. Please check output of `docker pull {}`",
                    imageName,
                    imageName,
                    lastException
                );
                throw new ContainerFetchException("Failed to pull image: " + imageName, lastException);
            }

            logger.info("Image {} pull took {}", dockerImageName.get(), Duration.between(startedAt, Instant.now()));
            LocalImagesCache.INSTANCE.refreshCache(imageName);
            return dockerImageName.get();
        } catch (DockerClientException e) {
            throw new ContainerFetchException("Failed to get Docker client for " + imageName, e);
        }
    }

    private Callable<Boolean> tryImagePullCommand(
        PullImageCmd pullImageCmd,
        Logger logger,
        AtomicReference<String> dockerImageName,
        DockerImageName imageName,
        AtomicReference<Exception> lastFailure,
        Instant lastRetryAllowed
    ) {
        return () -> {
            try {
                pullWithX86Fallback(pullImageCmd, logger);
                dockerImageName.set(imageName.asCanonicalNameString());
                return true;
            } catch (InterruptedException | InternalServerErrorException e) {
                // these classes of exception often relate to timeout/connection errors so should be retried
                lastFailure.set(e);
                logger.warn(
                    "Retrying pull for image: {} ({}s remaining)",
                    imageName,
                    Duration.between(Instant.now(), lastRetryAllowed).getSeconds()
                );
                return false;
            }
        };
    }

    private TimeLimitedLoggedPullImageResultCallback pullWithX86Fallback(PullImageCmd pullImageCmd, Logger logger)
        throws InterruptedException {
        try {
            return pullImageCmd.exec(new TimeLimitedLoggedPullImageResultCallback(logger)).awaitCompletion();
        } catch (DockerClientException e) {
            // Try to fallback to x86
            return pullImageCmd
                .withPlatform("linux/amd64")
                .exec(new TimeLimitedLoggedPullImageResultCallback(logger))
                .awaitCompletion();
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
