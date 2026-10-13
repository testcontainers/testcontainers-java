package org.testcontainers.images.retry;

import com.github.dockerjava.api.exception.InternalServerErrorException;
import org.junit.jupiter.api.Test;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ImagePullRetryPolicyTest {

    private final DockerImageName imageName = DockerImageName.parse("any/image:latest");

    @Test
    public void shouldNotRetryWhenUsingFailFastPullRetryPolicy() {
        ImagePullRetryPolicy policy = PullRetryPolicy.failFast();
        policy.pullStarted();
        assertThat(policy.shouldRetry(imageName, new Exception())).isFalse();
    }

    @Test
    public void shouldFailIfTheConfiguredDurationIsNegativeWhenUsingLimitedDurationPullRetryPolicy() {
        assertThatThrownBy(() -> PullRetryPolicy.limitedDuration(Duration.ofMinutes(-1)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("should not be negative");
    }

    @Test
    public void shouldFailIfPullStartedIsNotBeingCalledBeforeShouldRetryWhenUsingLimitedDurationPullRetryPolicy() {
        ImagePullRetryPolicy policy = PullRetryPolicy.limitedDuration(Duration.ofMinutes(1));
        assertThatThrownBy(() -> policy.shouldRetry(imageName, new Exception()))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Please, check that pullStarted has been called.");
    }

    @Test
    public void shouldRetryDuringTheConfiguredAmountOfTimeWhenUsingLimitedDurationPullRetryPolicy() throws InterruptedException {
        Duration maxAllowedDuration = Duration.ofMillis(100);
        ImagePullRetryPolicy policy = PullRetryPolicy.limitedDuration(maxAllowedDuration);
        policy.pullStarted();

        assertThat(policy.shouldRetry(imageName, new Exception())).isTrue();

        Thread.sleep(maxAllowedDuration.toMillis() + 50);

        assertThat(policy.shouldRetry(imageName, new Exception())).isFalse();
    }

    @Test
    public void shouldFailIfTheConfiguredNumberOfAttemptsIsNegativeWhenUsingNoOfAttemptsPullRetryPolicy() {
        assertThatThrownBy(() -> PullRetryPolicy.noOfAttempts(-1))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("should not be negative");
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
