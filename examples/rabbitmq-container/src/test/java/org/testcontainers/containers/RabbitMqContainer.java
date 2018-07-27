package org.testcontainers.containers;

import org.jetbrains.annotations.NotNull;
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;
import org.testcontainers.containers.wait.strategy.WaitStrategy;

import java.time.Duration;


/**
 * By default this {@link Container} uses the official RabbitMQ ({@code rabbitmq}) image from
 * <a href="https://hub.docker.com/_/rabbitmq/">DockerHub</a>. If you need to use a custom RabbitMQ
 * image, you can provide the full image name as well.
 *
 * @author Stefan Ludwig
 */
public class RabbitMqContainer extends GenericContainer<RabbitMqContainer> {

    private static final String DEFAULT_LOG_MESSAGE_REGEX = ".*Server startup complete.*\n";
    private static final int DEFAULT_STARTUP_TIMEOUT = 30;

    /**
     * This is the internal port on which RabbitMQ is running inside the container.
     * <p>
     * You can use this constant in case you want to map an explicit public port to it
     * instead of the default random port. This can be done using methods like
     * {@link #setPortBindings(java.util.List)}.
     */
    public static final int RABBITMQ_PORT = 5672;
    public static final String DEFAULT_IMAGE_AND_TAG = "rabbitmq:3.7";

    /**
     * Creates a new {@link RabbitMqContainer} with the {@value DEFAULT_IMAGE_AND_TAG} image.
     */
    public RabbitMqContainer() {
        this(DEFAULT_IMAGE_AND_TAG);
    }

    /**
     * Creates a new {@link RabbitMqContainer} with the given {@code 'image'}.
     *
     * @param image the image (e.g. {@value DEFAULT_IMAGE_AND_TAG}) to use
     */
    public RabbitMqContainer(@NotNull String image) {
        super(image);
        addExposedPort(RABBITMQ_PORT);
        setWaitStrategy(createWaitStrategy());
    }

    /**
     * This method can be overridden in order to change the {@link WaitStrategy}
     * used to determine whether or not the container is ready to be used.
     * <p>
     * Per default a {@link LogMessageWaitStrategy} with a pattern provided by
     * {@link #getContainerReadyLogRegEx()} and a startup timeout provided by
     * {@link #getStartupTimeout()} is used.
     *
     * @return the {@link WaitStrategy} to use
     */
    @NotNull
    protected WaitStrategy createWaitStrategy() {
        return new LogMessageWaitStrategy() //
            .withRegEx(getContainerReadyLogRegEx()) //
            .withStartupTimeout(getStartupTimeout());
    }

    /**
     * This method can be overridden in order to change the RegEx of the default
     * {@link LogMessageWaitStrategy} used to determine whether or not the container
     * is ready to be used.
     * <p>
     * The default pattern is {@value DEFAULT_LOG_MESSAGE_REGEX}.
     *
     * @return the log pattern to use
     */
    @NotNull
    protected String getContainerReadyLogRegEx() {
        return DEFAULT_LOG_MESSAGE_REGEX;
    }

    /**
     * This method can be overridden in order to change the default startup timeout
     * used when determining whether or not the container is ready to be used.
     * <p>
     * The default is {@value DEFAULT_STARTUP_TIMEOUT} seconds.
     *
     * @return the timeout {@link Duration}
     */
    @NotNull
    protected Duration getStartupTimeout() {
        return Duration.ofSeconds(DEFAULT_STARTUP_TIMEOUT);
    }

    /**
     * Returns the actual public port of the internal RabbitMQ port ({@value RABBITMQ_PORT}).
     *
     * @return the public port of this container
     * @see #getMappedPort(int)
     */
    @NotNull
    public Integer getPort() {
        return getMappedPort(RABBITMQ_PORT);
    }

}
