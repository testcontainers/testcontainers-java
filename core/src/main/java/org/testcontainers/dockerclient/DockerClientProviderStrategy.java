package org.testcontainers.dockerclient;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Info;
import com.github.dockerjava.api.model.Network;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.core.RemoteApiVersion;
import com.github.dockerjava.okhttp.OkDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import com.github.dockerjava.zerodep.ZerodepDockerHttpClient;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Throwables;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.rnorth.ducttape.TimeoutException;
import org.rnorth.ducttape.ratelimits.RateLimiter;
import org.rnorth.ducttape.ratelimits.RateLimiterBuilder;
import org.rnorth.ducttape.unreliables.Unreliables;
import org.testcontainers.utility.TestcontainersConfiguration;

import java.net.URI;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

/**
 * Mechanism to find a viable Docker client configuration according to the host system environment.
 */
@Slf4j
public abstract class DockerClientProviderStrategy {

    @Getter(lazy = true)
    private final DockerClient dockerClient = getClientForConfig(getTransportConfig());

    private String dockerHostIpAddress;

    private final RateLimiter PING_RATE_LIMITER = RateLimiterBuilder.newBuilder()
            .withRate(10, TimeUnit.SECONDS)
            .withConstantThroughput()
            .build();

    private static final AtomicBoolean FAIL_FAST_ALWAYS = new AtomicBoolean(false);

    /**
     * @return a short textual description of the strategy
     */
    public abstract String getDescription();

    protected boolean isApplicable() {
        return true;
    }

    protected boolean isPersistable() {
        return true;
    }

    /**
     * @return highest to lowest priority value
     */
    protected int getPriority() {
        return 0;
    }

    /**
     * @throws InvalidConfigurationException if this strategy fails
     */
    public abstract TransportConfig getTransportConfig() throws InvalidConfigurationException;

    /**
     * @return a usable, tested, Docker client configuration for the host system environment
     *
     * @deprecated use {@link #getDockerClient()}
     */
    @Deprecated
    public DockerClient getClient() {
        DockerClient dockerClient = getDockerClient();
        try {
            Unreliables.retryUntilSuccess(30, TimeUnit.SECONDS, () -> {
                return PING_RATE_LIMITER.getWhenReady(() -> {
                    log.debug("Pinging docker daemon...");
                    dockerClient.pingCmd().exec();
                    log.debug("Pinged");
                    return true;
                });
            });
        } catch (TimeoutException e) {
            IOUtils.closeQuietly(dockerClient);
            throw e;
        }
        return dockerClient;
    }

    /**
     * Determine the right DockerClientConfig to use for building clients by trial-and-error.
     *
     * @return a working DockerClientConfig, as determined by successful execution of a ping command
     */
    public static DockerClientProviderStrategy getFirstValidStrategy(List<DockerClientProviderStrategy> strategies) {

        if (FAIL_FAST_ALWAYS.get()) {
            throw new IllegalStateException("Previous attempts to find a Docker environment failed. Will not retry. Please see logs and check configuration");
        }

        List<String> configurationFailures = new ArrayList<>();

        return Stream
                .concat(
                        Stream
                                .of(TestcontainersConfiguration.getInstance().getDockerClientStrategyClassName())
                                .filter(Objects::nonNull)
                                .flatMap(it -> {
                                    try {
                                        Class<? extends DockerClientProviderStrategy> strategyClass = (Class) Thread.currentThread().getContextClassLoader().loadClass(it);
                                        return Stream.of(strategyClass.newInstance());
                                    } catch (ClassNotFoundException e) {
                                        log.warn("Can't instantiate a strategy from {} (ClassNotFoundException). " +
                                                "This probably means that cached configuration refers to a client provider " +
                                                "class that is not available in this version of Testcontainers. Other " +
                                                "strategies will be tried instead.", it);
                                        return Stream.empty();
                                    } catch (InstantiationException | IllegalAccessException e) {
                                        log.warn("Can't instantiate a strategy from {}", it, e);
                                        return Stream.empty();
                                    }
                                })
                                // Ignore persisted strategy if it's not persistable anymore
                                .filter(DockerClientProviderStrategy::isPersistable)
                                .peek(strategy -> log.info("Loaded {} from ~/.testcontainers.properties, will try it first", strategy.getClass().getName())),
                        strategies
                                .stream()
                                .filter(DockerClientProviderStrategy::isApplicable)
                                .sorted(Comparator.comparing(DockerClientProviderStrategy::getPriority).reversed())
                )
                .flatMap(strategy -> {
                    try {
                        DockerClient dockerClient = strategy.getDockerClient();

                        Info info;
                        try {
                            info = Unreliables.retryUntilSuccess(30, TimeUnit.SECONDS, () -> {
                                return strategy.PING_RATE_LIMITER.getWhenReady(() -> {
                                    log.debug("Pinging docker daemon...");
                                    return dockerClient.infoCmd().exec();
                                });
                            });
                        } catch (TimeoutException e) {
                            IOUtils.closeQuietly(dockerClient);
                            throw e;
                        }
                        log.info("Found Docker environment with {}", strategy.getDescription());
                        log.debug(
                            "Transport type: '{}', Docker host: '{}'",
                            TestcontainersConfiguration.getInstance().getTransportType(),
                            strategy.getTransportConfig().getDockerHost()
                        );

                        log.debug("Checking Docker OS type for {}", strategy.getDescription());
                        String osType = info.getOsType();
                        if (StringUtils.isBlank(osType)) {
                            log.warn("Could not determine Docker OS type");
                        } else if (!osType.equals("linux")) {
                            log.warn("{} is currently not supported", osType);
                            throw new InvalidConfigurationException(osType + " containers are currently not supported");
                        }

                        if (strategy.isPersistable()) {
                            TestcontainersConfiguration.getInstance().updateUserConfig("docker.client.strategy", strategy.getClass().getName());
                        }

                        return Stream.of(strategy);
                    } catch (Exception | ExceptionInInitializerError | NoClassDefFoundError e) {
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

                        log.debug(failureDescription);
                        return Stream.empty();
                    }
                })
                .findAny()
                .orElseThrow(() -> {
                    log.error("Could not find a valid Docker environment. Please check configuration. Attempted configurations were:");
                    for (String failureMessage : configurationFailures) {
                        log.error("    " + failureMessage);
                    }
                    log.error("As no valid configuration was found, execution cannot continue");

                    FAIL_FAST_ALWAYS.set(true);
                    return new IllegalStateException("Could not find a valid Docker environment. Please see logs and check configuration");
                });
    }

    public static DockerClient getClientForConfig(TransportConfig transportConfig) {
        final DockerHttpClient dockerHttpClient;

        String transportType = TestcontainersConfiguration.getInstance().getTransportType();
        switch (transportType) {
            case "okhttp":
                dockerHttpClient = new OkDockerHttpClient.Builder()
                    .dockerHost(transportConfig.getDockerHost())
                    .sslConfig(transportConfig.getSslConfig())
                    .build();
                break;
            case "httpclient5":
                dockerHttpClient = new ZerodepDockerHttpClient.Builder()
                    .dockerHost(transportConfig.getDockerHost())
                    .sslConfig(transportConfig.getSslConfig())
                    .build();
                break;
            default:
                throw new IllegalArgumentException("Unknown transport type '" + transportType + "'");
        }

        DefaultDockerClientConfig.Builder configBuilder = DefaultDockerClientConfig.createDefaultConfigBuilder();

        if (configBuilder.build().getApiVersion() == RemoteApiVersion.UNKNOWN_VERSION) {
            configBuilder.withApiVersion(RemoteApiVersion.VERSION_1_30);
        }
        return DockerClientImpl.getInstance(
            new AuthDelegatingDockerClientConfig(
                configBuilder
                    .withDockerHost(transportConfig.getDockerHost().toString())
                    .build()
            ),
            dockerHttpClient
        );
    }

    public synchronized String getDockerHostIpAddress() {
        if (dockerHostIpAddress == null) {
            dockerHostIpAddress = resolveDockerHostIpAddress(getDockerClient(), getTransportConfig().getDockerHost());
        }
        return dockerHostIpAddress;
    }

    @VisibleForTesting
    static String resolveDockerHostIpAddress(DockerClient client, URI dockerHost) {
        String hostOverride = System.getenv("TESTCONTAINERS_HOST_OVERRIDE");
        if (!StringUtils.isBlank(hostOverride)) {
            return hostOverride;
        }

        switch (dockerHost.getScheme()) {
            case "http":
            case "https":
            case "tcp":
                return dockerHost.getHost();
            case "unix":
            case "npipe":
                if (DockerClientConfigUtils.IN_A_CONTAINER) {
                    return client.inspectNetworkCmd()
                        .withNetworkId("bridge")
                        .exec()
                        .getIpam()
                        .getConfig()
                        .stream()
                        .filter(it -> it.getGateway() != null)
                        .findAny()
                        .map(Network.Ipam.Config::getGateway)
                        .orElseGet(() -> {
                            return DockerClientConfigUtils.getDefaultGateway().orElse("localhost");
                        });
                }
                return "localhost";
            default:
                return null;
        }
    }
}
