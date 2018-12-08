package org.testcontainers.images;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.ListImagesCmd;
import com.github.dockerjava.api.exception.DockerClientException;
import com.github.dockerjava.api.model.Image;
import lombok.NonNull;
import lombok.ToString;
import org.slf4j.Logger;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.ContainerFetchException;
import org.testcontainers.utility.*;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@ToString
public class RemoteDockerImage extends LazyFuture<String> {

    private static final Set<DockerImageName> AVAILABLE_IMAGE_NAME_CACHE = new HashSet<>();

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

            logger.info("Pulling docker image: {}. Please be patient; this may take some time but only needs to be done once.", imageName);

            // The image is not available locally - pull it
            try {
                final LoggedTimeLimitedPullImageResultCallback callback = new LoggedTimeLimitedPullImageResultCallback(logger);
                dockerClient
                    .pullImageCmd(imageName.getUnversionedPart())
                    .withTag(imageName.getVersionPart())
                    .exec(callback);
                callback.awaitCompletion();
                AVAILABLE_IMAGE_NAME_CACHE.add(imageName);
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
