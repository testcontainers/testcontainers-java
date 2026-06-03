package org.testcontainers.dockerclient;

import com.github.dockerjava.core.LocalDirectorySSLConfig;
import com.github.dockerjava.transport.SSLConfig;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Picks the Docker endpoint declared by the active Docker CLI context.
 * <p>
 * Resolution order matches {@code docker(1)}: {@code DOCKER_HOST} → {@code DOCKER_CONTEXT} →
 * {@code currentContext} in {@code $DOCKER_CONFIG/config.json}. When {@code DOCKER_HOST} is set
 * the CLI bypasses named contexts, and so does this strategy — the
 * {@link EnvironmentAndSystemPropertyClientProviderStrategy} owns that case. When no context
 * resolves to anything other than the built-in {@code default}, this strategy steps aside and lets
 * the local-socket strategies handle the platform default.
 * <p>
 * An explicit context can also be requested by name via
 * {@link #DockerContextClientProviderStrategy(String)}; the SPI uses the no-arg constructor and
 * resolves the active context dynamically.
 *
 * @deprecated this class is used by the SPI and should not be used directly
 */
@Slf4j
@Deprecated
public final class DockerContextClientProviderStrategy extends DockerClientProviderStrategy {

    public static final int PRIORITY = EnvironmentAndSystemPropertyClientProviderStrategy.PRIORITY - 10;

    private final Path dockerConfigDir;

    @Nullable
    private final String requestedContextName;

    @Getter(lazy = true)
    @Nullable
    private final DockerContextResolver.DockerContextEndpoint endpoint = resolveEndpoint();

    public DockerContextClientProviderStrategy() {
        this(DockerContextResolver.defaultDockerConfigDir(), null);
    }

    /**
     * Resolves the Docker endpoint for the supplied context name, bypassing the
     * {@code DOCKER_HOST}/{@code DOCKER_CONTEXT}/{@code currentContext} fallback chain.
     */
    public DockerContextClientProviderStrategy(String contextName) {
        this(DockerContextResolver.defaultDockerConfigDir(), contextName);
    }

    DockerContextClientProviderStrategy(Path dockerConfigDir, @Nullable String requestedContextName) {
        this.dockerConfigDir = dockerConfigDir;
        this.requestedContextName = requestedContextName;
    }

    @Nullable
    private DockerContextResolver.DockerContextEndpoint resolveEndpoint() {
        String contextName = requestedContextName != null
            ? requestedContextName
            : DockerContextResolver.resolveCurrentContextName(dockerConfigDir);
        if (contextName == null) {
            return null;
        }
        return DockerContextResolver.resolveEndpoint(dockerConfigDir, contextName);
    }

    @Override
    protected boolean isApplicable() {
        DockerContextResolver.DockerContextEndpoint endpoint = getEndpoint();
        if (endpoint == null) {
            return false;
        }
        String scheme = endpoint.getHost().getScheme();
        if (scheme == null) {
            return false;
        }
        switch (scheme) {
            case "unix":
            case "npipe":
            case "tcp":
            case "http":
            case "https":
                return true;
            default:
                log.debug(
                    "Docker context '{}' uses unsupported scheme '{}'; skipping",
                    endpoint.getContextName(),
                    scheme
                );
                return false;
        }
    }

    @Override
    public TransportConfig getTransportConfig() throws InvalidConfigurationException {
        DockerContextResolver.DockerContextEndpoint endpoint = getEndpoint();
        if (endpoint == null) {
            throw new InvalidConfigurationException("No Docker context endpoint resolved");
        }
        URI host = endpoint.getHost();
        if ("unix".equals(host.getScheme())) {
            Path socketPath = java.nio.file.Paths.get(host.getPath());
            if (!Files.exists(socketPath)) {
                throw new InvalidConfigurationException(
                    "Docker context '" +
                    endpoint.getContextName() +
                    "' points at " +
                    socketPath +
                    " but the socket does not exist"
                );
            }
        }
        TransportConfig.TransportConfigBuilder builder = TransportConfig.builder().dockerHost(host);
        SSLConfig sslConfig = buildSslConfig(endpoint);
        if (sslConfig != null) {
            builder.sslConfig(sslConfig);
        }
        return builder.build();
    }

    @Nullable
    private SSLConfig buildSslConfig(DockerContextResolver.DockerContextEndpoint endpoint) {
        Path tlsDir = endpoint.getTlsMaterialDir();
        if (tlsDir == null) {
            return null;
        }
        return new LocalDirectorySSLConfig(tlsDir.toString());
    }

    @Override
    protected int getPriority() {
        return PRIORITY;
    }

    @Override
    public String getDescription() {
        DockerContextResolver.DockerContextEndpoint endpoint = getEndpoint();
        if (endpoint == null) {
            return "Docker CLI context (none)";
        }
        return "Docker CLI context '" + endpoint.getContextName() + "' (" + endpoint.getHost() + ")";
    }

    @Override
    protected boolean isPersistable() {
        return false;
    }
}
