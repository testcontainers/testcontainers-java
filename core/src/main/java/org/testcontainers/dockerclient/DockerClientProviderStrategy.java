package org.testcontainers.dockerclient;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.netty.DockerCmdExecFactoryImpl;
import com.google.common.base.Throwables;
import org.jetbrains.annotations.Nullable;
import org.rnorth.ducttape.ratelimits.RateLimiter;
import org.rnorth.ducttape.ratelimits.RateLimiterBuilder;
import org.rnorth.ducttape.unreliables.Unreliables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Mechanism to find a viable Docker client configuration according to the host system environment.
 */
public abstract class DockerClientProviderStrategy {

    protected DockerClient client;
    protected DockerClientConfig config;

    private static final RateLimiter PING_RATE_LIMITER = RateLimiterBuilder.newBuilder()
            .withRate(2, TimeUnit.SECONDS)
            .withConstantThroughput()
            .build();

    /**
     * @throws InvalidConfigurationException if this strategy fails
     */
    public abstract void test() throws InvalidConfigurationException;

    /**
     * @return a short textual description of the strategy
     */
    public abstract String getDescription();

    protected static final Logger LOGGER = LoggerFactory.getLogger(DockerClientProviderStrategy.class);

    /**
     * Determine the right DockerClientConfig to use for building clients by trial-and-error.
     *
     * @return a working DockerClientConfig, as determined by successful execution of a ping command
     */
    public static DockerClientProviderStrategy getFirstValidStrategy(List<DockerClientProviderStrategy> strategies) {
        List<String> configurationFailures = new ArrayList<>();

        for (DockerClientProviderStrategy strategy : strategies) {
            try {
                LOGGER.info("Looking for Docker environment. Trying {}", strategy.getDescription());
                strategy.test();
                return strategy;
            } catch (Exception | ExceptionInInitializerError e) {
                @Nullable String throwableMessage = e.getMessage();
                @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
                Throwable rootCause = Throwables.getRootCause(e);
                @Nullable String rootCauseMessage = rootCause.getMessage();

                String failureDescription;
                if (throwableMessage != null && throwableMessage.equals(rootCauseMessage)) {
                    failureDescription = String.format("%s: failed with exception %s (%s)",
                            strategy.getClass().getSimpleName(),
                            e.getClass().getSimpleName(),
                            throwableMessage);
                } else {
                    failureDescription = String.format("%s: failed with exception %s (%s). Root cause %s (%s)",
                            strategy.getClass().getSimpleName(),
                            e.getClass().getSimpleName(),
                            throwableMessage,
                            rootCause.getClass().getSimpleName(),
                            rootCauseMessage
                    );
                }
                configurationFailures.add(failureDescription);

                LOGGER.debug(failureDescription);
            }
        }

        LOGGER.error("Could not find a valid Docker environment. Please check configuration. Attempted configurations were:");
        for (String failureMessage : configurationFailures) {
            LOGGER.error("    " + failureMessage);
        }
        LOGGER.error("As no valid configuration was found, execution cannot continue");

        throw new IllegalStateException("Could not find a valid Docker environment. Please see logs and check configuration");
    }

    /**
     * @return a usable, tested, Docker client configuration for the host system environment
     */
    public DockerClient getClient() {
        return client;
    }

    protected DockerClient getClientForConfig(DockerClientConfig config) {
        return DockerClientBuilder
                    .getInstance(config)
                    .withDockerCmdExecFactory(new DockerCmdExecFactoryImpl())
                    .build();
    }

    protected void ping(DockerClient client, int timeoutInSeconds) {
        Unreliables.retryUntilSuccess(timeoutInSeconds, TimeUnit.SECONDS, () -> {
            return PING_RATE_LIMITER.getWhenReady(() -> {
                LOGGER.debug("Pinging docker daemon...");
                client.pingCmd().exec();
                return true;
            });
        });
    }

    public String getDockerHostIpAddress() {
        return DockerClientConfigUtils.getDockerHostIpAddress(this.config);
    }


    class InvalidConfigurationException extends RuntimeException {

        public InvalidConfigurationException(String s) {
            super(s);
        }

        public InvalidConfigurationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
