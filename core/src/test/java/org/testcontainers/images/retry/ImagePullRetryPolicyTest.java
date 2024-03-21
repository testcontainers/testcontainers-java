package org.testcontainers.images.retry;

import com.github.dockerjava.api.exception.InternalServerErrorException;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.testcontainers.DockerRegistryContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

public class ImagePullRetryPolicyTest {

    @ClassRule
    public static DockerRegistryContainer registry = new DockerRegistryContainer();

    private final DockerImageName imageName = registry.createImage();

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void shouldNotRetryWhenUsingFailFastPullRetryPolicy() {
        ImagePullRetryPolicy policy = PullRetryPolicy.failFast();
        policy.pullStarted();
        assertThat(policy.shouldRetry(imageName, new Exception())).isFalse();
    }

    @Test
    public void shouldFailIfTheConfiguredDurationIsNegativeWhenUsingLimitedDurationPullRetryPolicy() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("should not be negative");
        PullRetryPolicy.limitedDuration(Duration.ofMinutes(-1));
    }

    @Test
    public void shouldFailIfPullStartedIsNotBeingCalledBeforeShouldRetryWhenUsingLimitedDurationPullRetryPolicy() {
        thrown.expect(IllegalStateException.class);
        thrown.expectMessage("Please, check that pullStarted has been called.");
        ImagePullRetryPolicy policy = PullRetryPolicy.limitedDuration(Duration.ofMinutes(1));
        policy.shouldRetry(imageName, new Exception());
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
    public void shouldFailIfTheConfiguredNumberOfAttemptsIsNegativeWhenUsingNoOfAttemptsPullRetryPolicy() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("should not be negative");
        PullRetryPolicy.noOfAttempts(-1);
    }

    @Test
    public void shouldRetryTheConfiguredNumberOfAttemptsWhenUsingNoOfAttemptsPullRetryPolicy() {
        int anyNumberOfOfAttemptsGreaterThanOrEqualToZero = 4;
        ImagePullRetryPolicy policy = PullRetryPolicy.noOfAttempts(anyNumberOfOfAttemptsGreaterThanOrEqualToZero);
        policy.pullStarted();
        while (anyNumberOfOfAttemptsGreaterThanOrEqualToZero-- > 0) {
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
            policy.shouldRetry(imageName, new InternalServerErrorException("The message is not important for the test"))
        )
            .isTrue();
    }
}
