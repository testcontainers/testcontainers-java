package org.testcontainers.images;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.exception.DockerClientException;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.core.command.PullImageResultCallback;
import java.util.HashMap;
import java.util.Map;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;
import org.slf4j.Logger;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.ContainerFetchException;
import org.testcontainers.containers.image.DockerJavaImageData;
import org.testcontainers.containers.image.ImageData;
import org.testcontainers.containers.image.pull.policy.DefaultPullPolicy;
import org.testcontainers.containers.image.pull.policy.ImagePullPolicy;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.DockerLoggerFactory;
import org.testcontainers.utility.LazyFuture;

@ToString
public class RemoteDockerImage extends LazyFuture<String> {

    /**
     * @deprecated this field will become private in a later release
     */
    @Deprecated
    public static final Map<DockerImageName, ImageData> AVAILABLE_IMAGES_CACHE = new HashMap<>();

    private DockerImageName imageName;

    @Setter
    private ImagePullPolicy imagePullPolicy;

    public RemoteDockerImage(@NonNull String dockerImageName, ImagePullPolicy pullPolicy) {
        imagePullPolicy = pullPolicy;
        imageName = new DockerImageName(dockerImageName);
    }

    public RemoteDockerImage(@NonNull String dockerImageName) {
        this(dockerImageName, new DefaultPullPolicy());
    }

    public RemoteDockerImage(@NonNull String repository, @NonNull String tag) {
        this(repository, tag, new DefaultPullPolicy());
    }

    public RemoteDockerImage(@NonNull String repository, @NonNull String tag, ImagePullPolicy pullPolicy) {
        imagePullPolicy = pullPolicy;
        imageName = new DockerImageName(repository, tag);
    }

    @Override
    protected final String resolve() {
        Logger logger = DockerLoggerFactory.getLogger(imageName.toString());

        DockerClient dockerClient = DockerClientFactory.instance().client();
        try {
            // Does our cache already know the image?
            if (AVAILABLE_IMAGES_CACHE.containsKey(imageName)) {
                logger.trace("{} is already in image name cache", imageName);
                return imageName.toString();
            }

            // Does the image exist in the local Docker cache?
            try {
                ImageData imageData = new DockerJavaImageData(
                    dockerClient.inspectImageCmd(imageName.toString()).exec());
                AVAILABLE_IMAGES_CACHE.putIfAbsent(imageName, imageData);
                if (!imagePullPolicy.shouldPull(imageData)) {
                    return imageName.toString();
                }
            } catch (NotFoundException ex) {
                logger.trace("Docker image {} not found locally", imageName);
            }

            logger.info("Pulling docker image: {}. Please be patient; this may take some time but only needs to be done once.", imageName);

            // The image is not available locally - pull it
            try {
                final PullImageResultCallback callback = new TimeLimitedLoggedPullImageResultCallback(logger);
                dockerClient
                    .pullImageCmd(imageName.getUnversionedPart())
                    .withTag(imageName.getVersionPart())
                    .exec(callback);
                callback.awaitCompletion();
                ImageData imageData = new DockerJavaImageData(dockerClient.inspectImageCmd(imageName.toString()).exec());
                AVAILABLE_IMAGES_CACHE.putIfAbsent(imageName, imageData);
            } catch (Exception e) {
                logger.error("Failed to pull image: {}. Please check output of `docker pull {}`", imageName, imageName);
                throw new ContainerFetchException("Failed to pull image: " + imageName, e);
            }

            return imageName.toString();
        } catch (DockerClientException e) {
            throw new ContainerFetchException("Failed to get Docker client for " + imageName, e);
        }
    }
}
