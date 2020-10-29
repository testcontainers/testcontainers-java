package org.testcontainers.utility;

import com.google.common.annotations.VisibleForTesting;
import lombok.extern.slf4j.Slf4j;

import java.util.ServiceLoader;
import java.util.function.Function;
import java.util.stream.StreamSupport;

import static java.util.Comparator.comparingInt;

/**
 * An image name substitutor converts a Docker image name, as may be specified in code, to an alternative name.
 * This is intended to provide a way to override image names, for example to enforce pulling of images from a private
 * registry.
 */
@Slf4j
public abstract class ImageNameSubstitutor implements Function<DockerImageName, DockerImageName> {

    @VisibleForTesting
    static ImageNameSubstitutor instance;

    public synchronized static ImageNameSubstitutor instance() {
        if (instance == null) {
            final ServiceLoader<ImageNameSubstitutor> serviceLoader = ServiceLoader.load(ImageNameSubstitutor.class);

            instance = StreamSupport.stream(serviceLoader.spliterator(), false)
                .peek(it -> log.debug("Found ImageNameSubstitutor using ServiceLoader: {} (priority {}) ", it, it.getPriority()))
                .max(comparingInt(ImageNameSubstitutor::getPriority))
                .map(ImageNameSubstitutor::wrapWithLogging)
                .orElseThrow(() -> new RuntimeException("Unable to find any ImageNameSubstitutor using ServiceLoader"));

            log.info("Using ImageNameSubstitutor: {}", instance);
        }

        return instance;
    }

    private static ImageNameSubstitutor wrapWithLogging(final ImageNameSubstitutor wrappedInstance) {
        return new LogWrappedImageNameSubstitutor(wrappedInstance);
    }

    /**
     * Substitute a {@link DockerImageName} for another, for example to replace a generic Docker Hub image name with a
     * private registry copy of the image.
     *
     * @param original original name to be replaced
     * @return a replacement name, or the original, as appropriate
     */
    public abstract DockerImageName apply(DockerImageName original);

    /**
     * Priority of this {@link ImageNameSubstitutor} compared to other instances that may be found by the service
     * loader. The highest priority instance found will always be used.
     *
     * @return a priority
     */
    protected abstract int getPriority();

    protected abstract String getDescription();

    /**
     * Wrapper substitutor which logs which substitutions have been performed.
     */
    static class LogWrappedImageNameSubstitutor extends ImageNameSubstitutor {
        @VisibleForTesting
        final ImageNameSubstitutor wrappedInstance;

        public LogWrappedImageNameSubstitutor(final ImageNameSubstitutor wrappedInstance) {
            this.wrappedInstance = wrappedInstance;
        }

        @Override
        public DockerImageName apply(final DockerImageName original) {
            final String className = wrappedInstance.getClass().getName();
            final DockerImageName replacementImage = wrappedInstance.apply(original);

            if (!replacementImage.equals(original)) {
                log.info("Using {} as a substitute image for {} (using image substitutor: {})", replacementImage.asCanonicalNameString(), original.asCanonicalNameString(), className);
                return replacementImage;
            } else {
                log.debug("Did not find a substitute image for {} (using image substitutor: {})", original.asCanonicalNameString(), className);
                return original;
            }
        }

        @Override
        protected int getPriority() {
            return wrappedInstance.getPriority();
        }

        @Override
        protected String getDescription() {
            return wrappedInstance.getDescription();
        }
    }
}
