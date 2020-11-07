package org.testcontainers.utility;

import com.google.common.annotations.VisibleForTesting;
import lombok.extern.slf4j.Slf4j;
import org.testcontainers.UnstableAPI;

import java.util.function.Function;

/**
 * An image name substitutor converts a Docker image name, as may be specified in code, to an alternative name.
 * This is intended to provide a way to override image names, for example to enforce pulling of images from a private
 * registry.
 * <p>
 * This is marked as @{@link UnstableAPI} as this API is new. While we do not think major changes will be required, we
 * will react to feedback if necessary.
 */
@Slf4j
@UnstableAPI
public abstract class ImageNameSubstitutor implements Function<DockerImageName, DockerImageName> {

    @VisibleForTesting
    static ImageNameSubstitutor instance;

    @VisibleForTesting
    static ImageNameSubstitutor defaultImplementation = new DefaultImageNameSubstitutor();

    public synchronized static ImageNameSubstitutor instance() {
        if (instance == null) {
            final String configuredClassName = TestcontainersConfiguration.getInstance().getImageSubstitutorClassName();

            if (configuredClassName != null) {
                log.debug("Attempting to instantiate an ImageNameSubstitutor with class: {}", configuredClassName);
                ImageNameSubstitutor configuredInstance;
                try {
                    configuredInstance = (ImageNameSubstitutor) Class.forName(configuredClassName).getConstructor().newInstance();
                } catch (Exception e) {
                    throw new IllegalArgumentException("Configured Image Substitutor could not be loaded: " + configuredClassName, e);
                }

                log.info("Found configured ImageNameSubstitutor: {}", configuredInstance.getDescription());

                instance = new ChainedImageNameSubstitutor(
                    wrapWithLogging(defaultImplementation),
                    wrapWithLogging(configuredInstance)
                );
            } else {
                instance = wrapWithLogging(defaultImplementation);
            }

            log.info("Image name substitution will be performed by: {}", instance.getDescription());
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
     * @return a human-readable description of the substitutor
     */
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
            final DockerImageName replacementImage = wrappedInstance.apply(original);

            if (!replacementImage.equals(original)) {
                log.info("Using {} as a substitute image for {} (using image substitutor: {})", replacementImage.asCanonicalNameString(), original.asCanonicalNameString(), wrappedInstance.getDescription());
                return replacementImage;
            } else {
                log.debug("Did not find a substitute image for {} (using image substitutor: {})", original.asCanonicalNameString(), wrappedInstance.getDescription());
                return original;
            }
        }

        @Override
        protected String getDescription() {
            return wrappedInstance.getDescription();
        }
    }

    /**
     * Wrapper substitutor that passes the original image name through a default substitutor and then the configured one
     */
    static class ChainedImageNameSubstitutor extends ImageNameSubstitutor {
        private final ImageNameSubstitutor defaultInstance;
        private final ImageNameSubstitutor configuredInstance;

        public ChainedImageNameSubstitutor(ImageNameSubstitutor defaultInstance, ImageNameSubstitutor configuredInstance) {
            this.defaultInstance = defaultInstance;
            this.configuredInstance = configuredInstance;
        }

        @Override
        public DockerImageName apply(DockerImageName original) {
            return defaultInstance.andThen(configuredInstance).apply(original);
        }

        @Override
        protected String getDescription() {
            return String.format(
                "Chained substitutor of '%s' and then '%s'",
                defaultInstance.getDescription(),
                configuredInstance.getDescription()
            );
        }
    }
}
