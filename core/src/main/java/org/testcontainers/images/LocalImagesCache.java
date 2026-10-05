package org.testcontainers.images;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectImageResponse;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.Image;
import com.google.common.annotations.VisibleForTesting;
import lombok.extern.slf4j.Slf4j;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.utility.DockerImageName;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
enum LocalImagesCache {
    INSTANCE;

    @VisibleForTesting
    final AtomicBoolean initialized = new AtomicBoolean(false);

    @VisibleForTesting
    final Map<DockerImageName, ImageData> cache = new ConcurrentHashMap<>();

    public ImageData get(DockerImageName imageName) {
        maybeInitCache(DockerClientFactory.instance().client());
        return cache.get(imageName);
    }

    public Optional<ImageData> refreshCache(DockerImageName imageName) {
        DockerClient dockerClient = DockerClientFactory.instance().client();
        if (!maybeInitCache(dockerClient)) {
            // Cache may be stale, trying inspectImageCmd...

            InspectImageResponse response = null;
            try {
                response = dockerClient.inspectImageCmd(imageName.asCanonicalNameString()).exec();
            } catch (NotFoundException e) {
                log.trace("Image {} not found", imageName, e);
            }
            if (response != null) {
                ImageData imageData = ImageData.from(response);
                cache.put(imageName, imageData);
                return Optional.of(imageData);
            } else {
                cache.remove(imageName);
                return Optional.empty();
            }
        }

        return Optional.ofNullable(cache.get(imageName));
    }

    private synchronized boolean maybeInitCache(DockerClient dockerClient) {
        if (!initialized.compareAndSet(false, true)) {
            return false;
        }

        if (Boolean.parseBoolean(System.getProperty("useFilter"))) {
            return false;
        }

        populateFromList(dockerClient.listImagesCmd().exec());

        return true;
    }

    @VisibleForTesting
    void populateFromList(List<Image> images) {
        for (Image image : images) {
            String[] repoTags = image.getRepoTags();
            String[] repoDigests = image.getRepoDigests();

            if (repoTags == null && repoDigests == null) {
                log.debug("repoTags and repoDigests are both null, skipping image: {}", image);
                continue;
            }

            Stream<String> tagNames = repoTags != null ? Stream.of(repoTags) : Stream.empty();
            Stream<String> digestNames = repoDigests != null ? Stream.of(repoDigests) : Stream.empty();

            cache.putAll(
                Stream
                    .concat(tagNames, digestNames)
                    // Protection against some edge case where local image repository tags end up with duplicates
                    // making toMap crash at merge time.
                    .distinct()
                    .collect(Collectors.toMap(DockerImageName::new, it -> ImageData.from(image)))
            );
        }
    }
}
