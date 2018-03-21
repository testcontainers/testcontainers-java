package org.testcontainers.images;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.ListImagesCmd;
import com.github.dockerjava.api.exception.DockerClientException;
import com.github.dockerjava.api.model.Image;
import com.github.dockerjava.core.command.PullImageResultCallback;
import lombok.NonNull;
import lombok.ToString;
import org.slf4j.Logger;
import org.slf4j.profiler.Profiler;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.ContainerFetchException;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.DockerLoggerFactory;
import org.testcontainers.utility.LazyFuture;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@ToString
public class RemoteDockerImage extends LazyFuture<String> {

    public static final Set<DockerImageName> AVAILABLE_IMAGE_NAME_CACHE = new HashSet<>();

    private DockerImageName imageName;

    public RemoteDockerImage(String dockerImageName) {
        imageName = new DockerImageName(dockerImageName);
    }

    public RemoteDockerImage(@NonNull String repository, @NonNull String tag) {
        imageName = new DockerImageName(repository, tag);
    }

    @Override
    protected final String resolve() {
        Profiler profiler = new Profiler("Rule creation - prefetch image");
        Logger logger = DockerLoggerFactory.getLogger(imageName.toString());
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
                if (AVAILABLE_IMAGE_NAME_CACHE.contains(imageName)) {
                    logger.trace("{} is already in image name cache", imageName);
                    break;
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
                    break;
                }

                // Log only on first attempt
                if (attempts == 0) {
                    logger.info("Pulling docker image: {}. Please be patient; this may take some time but only needs to be done once.", imageName);
                    profiler.start("Pull image");
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
                    callback.awaitSuccess();
                    AVAILABLE_IMAGE_NAME_CACHE.add(imageName);
                    break;
                } catch (DockerClientException e) {
                    lastException = e;
                }
            }

            return imageName.toString();
        } catch (DockerClientException e) {
            throw new ContainerFetchException("Failed to get Docker client for " + imageName, e);
        } finally {
            profiler.stop().log();
        }
    }
}
