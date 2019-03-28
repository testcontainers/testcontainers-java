package org.testcontainers.containers.image.pull.policy;

import java.util.concurrent.TimeUnit;
import lombok.experimental.UtilityClass;

/**
 * Convenience class with logic for building common {@link ImagePullPolicy} instances.
 *
 */
@UtilityClass
public class PullPolicy {

    /**
     * Convenience method for returning the {@link DefaultPullPolicy} default image pull policy
     * @return {@link ImagePullPolicy}
     */
    public static ImagePullPolicy getDefaultPullPolicy() {
        return new DefaultPullPolicy();
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
    public static ImagePullPolicy ageBased(long age, TimeUnit timeUnit) {
        return new AgeBasedPullPolicy(age, timeUnit);
    }

}
