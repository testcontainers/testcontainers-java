package org.testcontainers.images.retry;

import com.github.dockerjava.api.exception.InternalServerErrorException;
import org.junit.ClassRule;
import org.junit.Test;
import org.testcontainers.DockerRegistryContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

public class ImagePullRetryPolicyTest {

    @ClassRule
    public static DockerRegistryContainer registry = new DockerRegistryContainer();

    private final DockerImageName imageName = registry.createImage();

    @Test
    public void shouldNotRetryWhenUsingFailFastPullRetryPolicy() {
        ImagePullRetryPolicy policy = PullRetryPolicy.failFast();
        policy.pullStarted();
        assertThat(policy.shouldRetry(imageName, new Exception())).isFalse();
    }

    @Test
    public void shouldRetryDuringTheConfiguredAmountOfTimeWhenUsingLimitedDurationPullRetryPolicy() {
        Duration maxAllowedDuration = Duration.ofMillis(100);
        Instant lastRetryAllowed = Instant.now().plus(maxAllowedDuration);
        ImagePullRetryPolicy policy = PullRetryPolicy.limitedDuration(maxAllowedDuration);
        policy.pullStarted();
        while (Instant.now().isBefore(lastRetryAllowed)) {
            assertThat(policy.shouldRetry(imageName, new Exception())).isTrue();
        }

        assertThat(policy.shouldRetry(imageName, new Exception())).isFalse();
    }

    @Test
    public void shouldRetryTheConfiguredNumberOfAttemptsWhenUsingNoOfAttemptsPullRetryPolicy() {
        int allowedNoOfAttempts = 4;
        ImagePullRetryPolicy policy = PullRetryPolicy.noOfAttempts(allowedNoOfAttempts);
        policy.pullStarted();
        while (allowedNoOfAttempts-- > 0) {
            assertThat(policy.shouldRetry(imageName, new Exception())).isTrue();
        }

        assertThat(policy.shouldRetry(imageName, new Exception())).isFalse();
    }

    @Test
    public void shouldNotRetryWhenUsingDefaultPullRetryPolicyAndExceptionIsNotRetriable() {
        ImagePullRetryPolicy policy = PullRetryPolicy.defaultRetryPolicy();
        policy.pullStarted();
        assertThat(policy.shouldRetry(imageName, new Exception())).isFalse();
    }

    @Test
    public void shouldRetryWhenUsingDefaultPullRetryPolicyAndExceptionIsRetriableAndTheElapsedTimeIsUnderTheDefaut() {
        // I don't see a convenient way to test the default two minutes timeout: I rather
        // prefer to not test it
        ImagePullRetryPolicy policy = PullRetryPolicy.defaultRetryPolicy();
        policy.pullStarted();
        assertThat(policy.shouldRetry(imageName, new InterruptedException())).isTrue();
        assertThat(
            policy.shouldRetry(imageName,
                new InternalServerErrorException("The message is not important for the test"))
        ).isTrue();
    }
}
