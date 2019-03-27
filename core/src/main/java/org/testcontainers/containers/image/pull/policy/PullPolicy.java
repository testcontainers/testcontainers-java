package org.testcontainers.containers.image.pull.policy;

import java.util.concurrent.TimeUnit;

/**
 * Convenience class with logic for building common {@link ImagePullPolicy} instances.
 *
 */
public class PullPolicy {

    /**
     * Convenience method for returning the default image pull policy, which pulls the image only if it does not exist
     * @return {@link ImagePullPolicy}
     */
    public static ImagePullPolicy Default() {
        return new DefaultPullPolicy();
    }

    /**
     * Convenience method for returning the Always image pull policy, which pulls the image even if it exists locally.
     * Useful for obtaining the latest version of an image with a static tag, i.e. 'latest'
     * @return {@link ImagePullPolicy}
     */
    public static ImagePullPolicy Always() {
        return new AlwaysPullPolicy();
    }

    /**
     * Convenience method for returning an Age based image pull policy, which pulls the image if its created date is older than maxAge
     * @param age - Maximum age of image (based on image Created parameter)
     * @param timeUnit - The TimeUnit to use (MINUTES, HOURS, DAYS, etc.)
     * @return {@link ImagePullPolicy}
     */
    public static ImagePullPolicy AgeBased(long age, TimeUnit timeUnit) {
        return new AgeBasedPullPolicy(age, timeUnit);
    }

}
