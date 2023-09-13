package org.testcontainers.images;

import com.google.common.annotations.VisibleForTesting;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.testcontainers.utility.TestcontainersConfiguration;

import java.time.Duration;

/**
 * Convenience class with logic for building common {@link ImagePullPolicy} instances.
 *
 */
@Slf4j
@UtilityClass
public class PullPolicy {

    @VisibleForTesting
    static ImagePullPolicy instance;

    @VisibleForTesting
    static ImagePullPolicy defaultImplementation = new DefaultPullPolicy();

    /**
     * Convenience method for returning the {@link DefaultPullPolicy} default image pull policy
     * @return {@link ImagePullPolicy}
     */
    public static ImagePullPolicy defaultPolicy() {
        if (instance == null) {
            String imagePullPolicyClassName = TestcontainersConfiguration.getInstance().getImagePullPolicy();
            if (imagePullPolicyClassName != null) {
                log.debug("Attempting to instantiate an ImagePullPolicy with class: {}", imagePullPolicyClassName);
                ImagePullPolicy configuredInstance;
                try {
                    configuredInstance =
                        (ImagePullPolicy) Thread
                            .currentThread()
                            .getContextClassLoader()
                            .loadClass(imagePullPolicyClassName)
                            .getConstructor()
                            .newInstance();
                } catch (Exception e) {
                    throw new IllegalArgumentException(
                        "Configured Pull Policy could not be loaded: " + imagePullPolicyClassName,
                        e
                    );
                }

                log.info("Found configured Pull Policy: {}", configuredInstance.getClass());

                instance = configuredInstance;
            } else {
                instance = defaultImplementation;
            }
        }

        return instance;
    }

    /**
     * Convenience method for returning the {@link AlwaysPullPolicy} alwaysPull image pull policy
     * @return {@link ImagePullPolicy}
     */
    public static ImagePullPolicy alwaysPull() {
        return new AlwaysPullPolicy();
    }

    /**
     * Convenience method for returning an {@link AgeBasedPullPolicy} Age based image pull policy,
     * @return {@link ImagePullPolicy}
     */
    public static ImagePullPolicy ageBased(Duration maxAge) {
        return new AgeBasedPullPolicy(maxAge);
    }
}
