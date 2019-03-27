package org.testcontainers.images;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.ListImagesCmd;
import com.github.dockerjava.api.exception.DockerClientException;
import com.github.dockerjava.api.model.Image;
import com.github.dockerjava.core.command.PullImageResultCallback;
import java.util.AbstractMap.SimpleEntry;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.ToString;
import org.slf4j.Logger;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.ContainerFetchException;
import org.testcontainers.containers.image.ImageData;
import org.testcontainers.containers.image.pull.policy.DefaultPullPolicy;
import org.testcontainers.containers.image.pull.policy.ImagePullPolicy;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.DockerLoggerFactory;
import org.testcontainers.utility.LazyFuture;

@ToString
public class RemoteDockerImage extends LazyFuture<String> {

    public static final Map<DockerImageName, ImageData> AVAILABLE_IMAGES_CACHE = new HashMap<>();

    private DockerImageName imageName;

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
            int attempts = 0;
            Exception lastException = null;
            while (true) {
                // Does our cache already know the image?
                if (AVAILABLE_IMAGES_CACHE.containsKey(imageName)) {
                    logger.trace("{} is already in image name cache", imageName);
                    break;
                }

                // Update the cache
                ListImagesCmd listImagesCmd = dockerClient.listImagesCmd();

                if (Boolean.parseBoolean(System.getProperty("useFilter"))) {
                    listImagesCmd = listImagesCmd.withImageNameFilter(imageName.toString());
                }

                List<Image> updatedImages = listImagesCmd.exec();


                // Populate images cache
                updatedImages.stream()
                    .filter(i -> Objects.nonNull(i.getRepoTags()))
                    .flatMap(image -> Arrays.stream(image.getRepoTags())
                        .map(tag -> new SimpleEntry<>(tag, ImageData.from(image))))
                    .collect(Collectors.toMap(e -> new DockerImageName(e.getKey()), Entry::getValue, (t1,t2) -> t1, () -> AVAILABLE_IMAGES_CACHE));

                // And now?
                if (AVAILABLE_IMAGES_CACHE.containsKey(imageName) && !imagePullPolicy.shouldPull(AVAILABLE_IMAGES_CACHE.get(imageName))) {
                    logger.trace("{} is in image name cache following listing of images", imageName);
                    break;
                }

                // Log only on first attempt
                if (attempts == 0) {
                    logger.info("Pulling docker image: {}. Please be patient; this may take some time but only needs to be done once.", imageName);
                }

                if (attempts++ >= 3) {
                    logger.error("Retry limit reached while trying to pull image: {}. Please check output of `docker pull {}`", imageName, imageName);
                    throw new ContainerFetchException("Retry limit reached while trying to pull image: " + imageName, lastException);
                }

                // The image is not available locally - pull it
                try {
                    final PullImageResultCallback callback = new PullImageResultCallback();
                    dockerClient
                        .pullImageCmd(imageName.getUnversionedPart())
                        .withTag(imageName.getVersionPart())
                        .exec(callback);
                    callback.awaitCompletion();
                    AVAILABLE_IMAGES_CACHE.putIfAbsent(imageName,ImageData.from(dockerClient.inspectImageCmd(imageName.toString()).exec()));
                    break;
                } catch (Exception e) {
                    lastException = e;
                }
            }

            return imageName.toString();
        } catch (DockerClientException e) {
            throw new ContainerFetchException("Failed to get Docker client for " + imageName, e);
        }
    }
}
