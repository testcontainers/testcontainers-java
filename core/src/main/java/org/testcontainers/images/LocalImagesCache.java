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
            InspectImageResponse response = null;
            try {
                response = dockerClient.inspectImageCmd(imageName.asCanonicalNameString()).exec();
            } catch (NotFoundException e) {
                log.trace("Image {} not found", imageName, e);
            }
            if (response != null) {
                ImageData imageData = ImageData.from(response);
                cache.put(imageName, imageData);
                if (response.getRepoDigests() != null) {
                    for (String repoDigest : response.getRepoDigests()) {
                        if (repoDigest != null && !"<none>@<none>".equals(repoDigest)) {
                            try {
                                cache.put(DockerImageName.parse(repoDigest), imageData);
                            } catch (IllegalArgumentException ignored) {}
                        }
                    }
                }
                String imageId = response.getId();
                if (imageId != null) {
                    try {
                        cache.put(DockerImageName.parse(imageId), imageData);
                        if (imageId.startsWith("sha256:")) {
                            cache.put(DockerImageName.parse(imageId.substring(7)), imageData);
                        }
                    } catch (IllegalArgumentException ignored) {}
                }
                return Optional.of(imageData);
            } else {
                cache.remove(imageName);
                return Optional.empty();
            }
        }

        return Optional.ofNullable(cache.get(imageName));
    }

    @VisibleForTesting
    synchronized boolean maybeInitCache(DockerClient dockerClient) {
        if (!initialized.compareAndSet(false, true)) {
            return false;
        }

        if (Boolean.parseBoolean(System.getProperty("useFilter"))) {
            return false;
        }

        populateFromList(dockerClient.listImagesCmd().exec());

        return true;
    }

    private void populateFromList(List<Image> images) {
        for (Image image : images) {
            ImageData imageData = ImageData.from(image);

            if (image.getRepoTags() != null) {
                for (String repoTag : image.getRepoTags()) {
                    if (repoTag != null && !"<none>:<none>".equals(repoTag)) {
                        try {
                            cache.put(DockerImageName.parse(repoTag), imageData);
                        } catch (IllegalArgumentException e) {
                            log.debug("Failed to parse repoTag: {}", repoTag, e);
                        }
                    }
                }
            }

            if (image.getRepoDigests() != null) {
                for (String repoDigest : image.getRepoDigests()) {
                    if (repoDigest != null && !"<none>@<none>".equals(repoDigest)) {
                        try {
                            cache.put(DockerImageName.parse(repoDigest), imageData);
                        } catch (IllegalArgumentException e) {
                            log.debug("Failed to parse repoDigest: {}", repoDigest, e);
                        }
                    }
                }
            }

            String imageId = image.getId();
            if (imageId != null) {
                try {
                    cache.put(DockerImageName.parse(imageId), imageData);
                    if (imageId.startsWith("sha256:")) {
                        cache.put(DockerImageName.parse(imageId.substring(7)), imageData);
                    }
                } catch (IllegalArgumentException e) {
                    log.debug("Failed to parse image id: {}", imageId, e);
                }
            }
        }
    }
}
