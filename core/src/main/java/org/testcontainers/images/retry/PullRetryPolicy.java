package org.testcontainers.images.retry;

import lombok.experimental.UtilityClass;

import java.time.Duration;

/**
 * Convenience class with logic for building common {@link ImagePullRetryPolicy} instances.
 *
 */
@UtilityClass
public class PullRetryPolicy {

    /**
     * Convenience method for returning the {@link FailFastPullRetryPolicy} failFast image pull retry policy
     * @return {@link ImagePullRetryPolicy}
     */
    public static ImagePullRetryPolicy failFast() {
        return new FailFastPullRetryPolicy();
    }

    /**
     * Convenience method for returning the {@link NoOfAttemptsPullRetryPolicy} number of attempts based image pull
     * retry policy.
     * @return {@link ImagePullRetryPolicy}
     */
    public static ImagePullRetryPolicy noOfAttempts(int allowedNoOfAttempts) {
        return new NoOfAttemptsPullRetryPolicy(allowedNoOfAttempts);
    }

    /**
     * Convenience method for returning the {@link LimitedDurationPullRetryPolicy} duration image pull retry policy
     * @return {@link ImagePullRetryPolicy}
     */
    public static ImagePullRetryPolicy limitedDuration(Duration maxAllowedDuration) {
        return new LimitedDurationPullRetryPolicy(maxAllowedDuration);
    }

    /**
     * Convenience method for returning the {@link DefaultPullRetryPolicy} default image pull retry policy.
     * @return {@link ImagePullRetryPolicy}
     */
    public static ImagePullRetryPolicy defaultRetryPolicy() {
        return new DefaultPullRetryPolicy();
    }
}
