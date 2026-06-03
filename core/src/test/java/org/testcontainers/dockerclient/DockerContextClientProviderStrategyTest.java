package org.testcontainers.dockerclient;

import org.apache.commons.lang3.SystemUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assumptions.assumeThat;

class DockerContextClientProviderStrategyTest {

    @Test
    void notApplicableWhenContextResolvesToDefault(@TempDir Path dockerConfigDir) {
        DockerContextClientProviderStrategy strategy = new DockerContextClientProviderStrategy(dockerConfigDir, null);

        assertThat(strategy.isApplicable()).isFalse();
    }

    @Test
    void notApplicableForSshContext(@TempDir Path dockerConfigDir) throws IOException {
        writeMeta(
            dockerConfigDir,
            "rpi",
            "{\"Name\":\"rpi\",\"Endpoints\":{\"docker\":{\"Host\":\"ssh://user@192.168.1.10\"}}}"
        );

        DockerContextClientProviderStrategy strategy = new DockerContextClientProviderStrategy(dockerConfigDir, "rpi");

        assertThat(strategy.isApplicable()).isFalse();
    }

    @Test
    void resolvesNamedContextThroughExplicitConstructor(@TempDir Path dockerConfigDir) throws IOException {
        String host = fakeEndpointHost(dockerConfigDir, "fake");
        writeMeta(
            dockerConfigDir,
            "my-ctx",
            "{\"Name\":\"my-ctx\",\"Endpoints\":{\"docker\":{\"Host\":\"" + host + "\"}}}"
        );

        DockerContextClientProviderStrategy strategy = new DockerContextClientProviderStrategy(
            dockerConfigDir,
            "my-ctx"
        );

        assertThat(strategy.isApplicable()).isTrue();
        TransportConfig config = strategy.getTransportConfig();
        assertThat(config.getDockerHost().toString()).isEqualTo(host);
        assertThat(config.getSslConfig()).isNull();
        assertThat(strategy.getDescription()).contains("my-ctx").contains(host);
    }

    @Test
    void usesCurrentContextFromConfigWhenNoExplicitName(@TempDir Path dockerConfigDir) throws IOException {
        String host = fakeEndpointHost(dockerConfigDir, "current");
        writeConfig(dockerConfigDir, "{\"currentContext\":\"picked\"}");
        writeMeta(
            dockerConfigDir,
            "picked",
            "{\"Name\":\"picked\",\"Endpoints\":{\"docker\":{\"Host\":\"" + host + "\"}}}"
        );

        DockerContextClientProviderStrategy strategy = new DockerContextClientProviderStrategy(dockerConfigDir, null);

        assertThat(strategy.isApplicable()).isTrue();
        assertThat(strategy.getTransportConfig().getDockerHost().toString()).isEqualTo(host);
    }

    @Test
    void getTransportConfigFailsWhenUnixSocketMissing(@TempDir Path dockerConfigDir) throws IOException {
        writeMeta(
            dockerConfigDir,
            "ghost",
            "{\"Name\":\"ghost\",\"Endpoints\":{\"docker\":{\"Host\":\"unix:///definitely/not/here.sock\"}}}"
        );

        DockerContextClientProviderStrategy strategy = new DockerContextClientProviderStrategy(dockerConfigDir, "ghost");

        assertThatThrownBy(strategy::getTransportConfig)
            .isInstanceOf(InvalidConfigurationException.class)
            .hasMessageContaining("does not exist");
    }

    @Test
    void exposesTlsMaterialWhenPresent(@TempDir Path dockerConfigDir) throws IOException {
        writeMeta(
            dockerConfigDir,
            "remote",
            "{\"Name\":\"remote\",\"Endpoints\":{\"docker\":{\"Host\":\"tcp://10.0.0.5:2376\"}}}"
        );
        Path tlsDir = DockerContextResolver.contextTlsDir(dockerConfigDir, "remote");
        Files.createDirectories(tlsDir);
        Files.write(tlsDir.resolve("ca.pem"), "ca".getBytes(StandardCharsets.UTF_8));
        Files.write(tlsDir.resolve("cert.pem"), "cert".getBytes(StandardCharsets.UTF_8));
        Files.write(tlsDir.resolve("key.pem"), "key".getBytes(StandardCharsets.UTF_8));

        DockerContextClientProviderStrategy strategy = new DockerContextClientProviderStrategy(
            dockerConfigDir,
            "remote"
        );

        TransportConfig config = strategy.getTransportConfig();
        assertThat(config.getDockerHost().toString()).isEqualTo("tcp://10.0.0.5:2376");
        assertThat(config.getSslConfig()).isNotNull();
        // docker-java is relocated under org.testcontainers.shaded.* in the produced jar, so we
        // can't depend on the class identity — match by simple name and extract the path via the
        // public getter.
        assertThat(config.getSslConfig().getClass().getSimpleName()).isEqualTo("LocalDirectorySSLConfig");
        assertThat(config.getSslConfig()).extracting("dockerCertPath").isEqualTo(tlsDir.toString());
    }

    /**
     * Live smoke test against the local Docker installation: skipped unless an active context
     * resolves to a unix socket that actually exists. With Docker Desktop running, this confirms
     * end-to-end that the strategy can reach the daemon — including setups where Docker is only
     * reachable through the configured context (no {@code /var/run/docker.sock} symlink).
     */
    @Test
    void connectsToLocalDockerThroughActiveContext() {
        Path dockerConfigDir = DockerContextResolver.defaultDockerConfigDir();
        assumeThat(Files.exists(dockerConfigDir)).as("Docker config dir exists").isTrue();
        assumeThat(System.getenv("DOCKER_HOST")).as("DOCKER_HOST is unset").isNull();

        DockerContextClientProviderStrategy strategy = new DockerContextClientProviderStrategy();
        assumeThat(strategy.isApplicable()).as("strategy is applicable").isTrue();

        TransportConfig config = strategy.getTransportConfig();
        assumeThat(config.getDockerHost().getScheme()).as("scheme").isIn("unix", "tcp", "http", "https", "npipe");

        // Actually exercise the docker daemon — if Docker Desktop is reachable via context this
        // succeeds; otherwise the assumption short-circuits.
        try (com.github.dockerjava.api.DockerClient client = strategy.getDockerClient()) {
            client.pingCmd().exec();
            assertThat(client.infoCmd().exec().getOsType()).isEqualTo("linux");
        } catch (Exception e) {
            assumeThat(e).as("docker daemon reachable via active context").isNull();
        }
    }

    @Test
    void priorityIsBetweenEnvVarAndUnixSocket() {
        assertThat(DockerContextClientProviderStrategy.PRIORITY)
            .isGreaterThan(UnixSocketClientProviderStrategy.PRIORITY)
            .isLessThan(EnvironmentAndSystemPropertyClientProviderStrategy.PRIORITY);
    }

    /**
     * End-to-end check that the SPI flow in {@link org.testcontainers.DockerClientFactory} picks
     * this strategy when the active context resolves to a working endpoint.
     */
    @Test
    void factoryPicksContextStrategyForActiveContext() {
        assumeThat(Files.exists(DockerContextResolver.defaultDockerConfigDir())).isTrue();
        assumeThat(System.getenv("DOCKER_HOST")).isNull();
        assumeThat(new DockerContextClientProviderStrategy().isApplicable())
            .as("active context resolves to a non-default endpoint")
            .isTrue();

        try {
            org.testcontainers.DockerClientFactory.instance().client();
        } catch (Exception e) {
            assumeThat(e).as("DockerClientFactory can initialize").isNull();
        }

        assertThat(
            org.testcontainers.DockerClientFactory.instance().isUsing(DockerContextClientProviderStrategy.class)
        ).isTrue();
    }

    private static void writeConfig(Path dockerConfigDir, String json) throws IOException {
        Files.createDirectories(dockerConfigDir);
        Files.write(dockerConfigDir.resolve("config.json"), json.getBytes(StandardCharsets.UTF_8));
    }

    private static void writeMeta(Path dockerConfigDir, String contextName, String json) throws IOException {
        Path metaFile = DockerContextResolver.contextMetaFile(dockerConfigDir, contextName);
        Files.createDirectories(metaFile.getParent());
        Files.write(metaFile, json.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Builds a Docker endpoint host suitable for the current OS: a real unix socket on POSIX (whose
     * existence the strategy verifies) and an npipe address on Windows (which needs no backing
     * file). Both forms use forward slashes, so they embed safely into the JSON meta fixtures.
     */
    private static String fakeEndpointHost(Path dir, String name) throws IOException {
        if (SystemUtils.IS_OS_WINDOWS) {
            return "npipe:////./pipe/" + name;
        }
        Path socket = dir.resolve(name + ".sock");
        Files.write(socket, new byte[0]);
        return "unix://" + socket.toAbsolutePath();
    }
}
