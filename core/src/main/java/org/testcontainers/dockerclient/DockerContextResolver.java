package org.testcontainers.dockerclient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Resolves the Docker endpoint for a Docker CLI context.
 * <p>
 * Mirrors the resolution order used by the Docker CLI (see {@code cli/command/cli.go}):
 * <ol>
 *     <li>{@code DOCKER_HOST} env var — when set, the CLI forces the {@code default} context and
 *     does not consult any named context. This class therefore returns no endpoint in that case.</li>
 *     <li>{@code DOCKER_CONTEXT} env var.</li>
 *     <li>{@code currentContext} in {@code $DOCKER_CONFIG/config.json} (default
 *     {@code ~/.docker/config.json}).</li>
 *     <li>The built-in {@code default} context (no metadata file).</li>
 * </ol>
 * <p>
 * Named context metadata lives at
 * {@code $DOCKER_CONFIG/contexts/meta/<sha256(name)>/meta.json}; the per-endpoint TLS material,
 * when present, lives under {@code $DOCKER_CONFIG/contexts/tls/<sha256(name)>/docker/}.
 */
@Slf4j
public final class DockerContextResolver {

    public static final String DEFAULT_CONTEXT_NAME = "default";

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private DockerContextResolver() {}

    /**
     * @return the user's Docker config directory, honouring {@code DOCKER_CONFIG} if set, else
     *         {@code ~/.docker}.
     */
    public static Path defaultDockerConfigDir() {
        String override = System.getenv("DOCKER_CONFIG");
        if (!StringUtils.isBlank(override)) {
            return Paths.get(override);
        }
        return Paths.get(System.getProperty("user.home"), ".docker");
    }

    /**
     * Resolves the context name that the Docker CLI would pick, given the current process
     * environment and the supplied Docker config directory.
     *
     * @return the context name, or {@code null} if {@code DOCKER_HOST} is set (in which case the
     *         CLI bypasses named contexts entirely).
     */
    @Nullable
    public static String resolveCurrentContextName(Path dockerConfigDir) {
        return resolveCurrentContextName(dockerConfigDir, System::getenv);
    }

    @Nullable
    static String resolveCurrentContextName(Path dockerConfigDir, java.util.function.Function<String, String> env) {
        if (!StringUtils.isBlank(env.apply("DOCKER_HOST"))) {
            return null;
        }
        String fromEnv = env.apply("DOCKER_CONTEXT");
        if (!StringUtils.isBlank(fromEnv)) {
            return fromEnv;
        }
        String fromConfig = readCurrentContextFromConfig(dockerConfigDir);
        if (!StringUtils.isBlank(fromConfig)) {
            return fromConfig;
        }
        return DEFAULT_CONTEXT_NAME;
    }

    @Nullable
    private static String readCurrentContextFromConfig(Path dockerConfigDir) {
        Path configFile = dockerConfigDir.resolve("config.json");
        if (!Files.exists(configFile)) {
            return null;
        }
        try {
            JsonNode root = OBJECT_MAPPER.readTree(configFile.toFile());
            JsonNode current = root.get("currentContext");
            if (current == null || !current.isTextual()) {
                return null;
            }
            return current.asText();
        } catch (IOException e) {
            log.debug("Failed to read currentContext from {}", configFile, e);
            return null;
        }
    }

    /**
     * Reads the Docker endpoint for the named context.
     *
     * @param dockerConfigDir the Docker config directory (typically {@code ~/.docker}).
     * @param contextName the context name. The built-in {@code default} context has no metadata
     *                    file and returns {@code null}.
     * @return the resolved endpoint, or {@code null} for the {@code default} context.
     * @throws InvalidConfigurationException if the context is not the default one and its metadata
     *                                       cannot be read or is malformed.
     */
    @Nullable
    public static DockerContextEndpoint resolveEndpoint(Path dockerConfigDir, String contextName) {
        if (DEFAULT_CONTEXT_NAME.equals(contextName)) {
            return null;
        }
        Path metaFile = contextMetaFile(dockerConfigDir, contextName);
        if (!Files.exists(metaFile)) {
            throw new InvalidConfigurationException(
                "Docker context '" + contextName + "' has no metadata at " + metaFile
            );
        }
        JsonNode root;
        try {
            root = OBJECT_MAPPER.readTree(metaFile.toFile());
        } catch (IOException e) {
            throw new InvalidConfigurationException("Failed to read Docker context metadata at " + metaFile, e);
        }
        JsonNode dockerEndpoint = root.path("Endpoints").path("docker");
        JsonNode hostNode = dockerEndpoint.get("Host");
        if (hostNode == null || !hostNode.isTextual() || StringUtils.isBlank(hostNode.asText())) {
            throw new InvalidConfigurationException(
                "Docker context '" + contextName + "' does not declare a docker endpoint host"
            );
        }
        URI host;
        try {
            host = new URI(hostNode.asText());
        } catch (URISyntaxException e) {
            throw new InvalidConfigurationException(
                "Docker context '" + contextName + "' has an invalid host URI: " + hostNode.asText(),
                e
            );
        }
        boolean skipTlsVerify = dockerEndpoint.path("SkipTLSVerify").asBoolean(false);
        Path tlsDir = contextTlsDir(dockerConfigDir, contextName);
        Path resolvedTlsDir = Files.isDirectory(tlsDir) ? tlsDir : null;
        return new DockerContextEndpoint(contextName, host, resolvedTlsDir, skipTlsVerify);
    }

    static Path contextMetaFile(Path dockerConfigDir, String contextName) {
        return dockerConfigDir.resolve("contexts").resolve("meta").resolve(sha256(contextName)).resolve("meta.json");
    }

    static Path contextTlsDir(Path dockerConfigDir, String contextName) {
        return dockerConfigDir.resolve("contexts").resolve("tls").resolve(sha256(contextName)).resolve("docker");
    }

    private static String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    @Value
    public static class DockerContextEndpoint {

        String contextName;

        URI host;

        @Nullable
        Path tlsMaterialDir;

        boolean skipTlsVerify;
    }
}
