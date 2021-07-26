package org.testcontainers.images;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.DockerLoggerFactory;

@Slf4j
public abstract class AbstractImagePullPolicy implements ImagePullPolicy {

    private static final LocalImagesCache LOCAL_IMAGES_CACHE = LocalImagesCache.INSTANCE;

    @Override
    public boolean shouldPull(DockerImageName imageName) {
        Logger logger = DockerLoggerFactory.getLogger(imageName.asCanonicalNameString());

        // Does our cache already know the image?
        ImageData cachedImageData = LOCAL_IMAGES_CACHE.get(imageName);
        if (cachedImageData != null) {
            logger.trace("{} is already in image name cache", imageName);
        } else {
            logger.debug("{} is not in image name cache, updating...", imageName);
            // Was not in cache, inspecting...
            cachedImageData = LOCAL_IMAGES_CACHE.refreshCache(imageName).orElse(null);

            if (cachedImageData == null) {
                log.debug("Not available locally, should pull image: {}", imageName);
                return true;
            }
        }

        if (shouldPullCached(imageName, cachedImageData)) {
            log.debug("Should pull locally available image: {}", imageName);
            return true;
        } else {
            log.debug("Using locally available and not pulling image: {}", imageName);
            return false;
        }

    }

    /**
     * Implement this method to decide whether a locally available image should be pulled
     * (e.g. to always pull images, or to pull them after some duration of time)
     *
     * @return {@code true} to update the locally available image, {@code false} to use local instead
     */
    abstract protected boolean shouldPullCached(DockerImageName imageName, ImageData localImageData);
}
