package org.testcontainers.dockerclient;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DockerContextResolverTest {

    @Test
    void resolvesDefaultWhenConfigAbsent(@TempDir Path dockerConfigDir) {
        String name = DockerContextResolver.resolveCurrentContextName(dockerConfigDir, k -> null);
        assertThat(name).isEqualTo(DockerContextResolver.DEFAULT_CONTEXT_NAME);
    }

    @Test
    void resolvesCurrentContextFromConfig(@TempDir Path dockerConfigDir) throws IOException {
        writeConfig(dockerConfigDir, "{\"currentContext\":\"desktop-linux\"}");

        String name = DockerContextResolver.resolveCurrentContextName(dockerConfigDir, k -> null);

        assertThat(name).isEqualTo("desktop-linux");
    }

    @Test
    void dockerContextEnvOverridesConfig(@TempDir Path dockerConfigDir) throws IOException {
        writeConfig(dockerConfigDir, "{\"currentContext\":\"desktop-linux\"}");

        String name = DockerContextResolver.resolveCurrentContextName(
            dockerConfigDir,
            mapEnv("DOCKER_CONTEXT", "orbstack")
        );

        assertThat(name).isEqualTo("orbstack");
    }

    @Test
    void dockerHostEnvBypassesContexts(@TempDir Path dockerConfigDir) throws IOException {
        writeConfig(dockerConfigDir, "{\"currentContext\":\"desktop-linux\"}");

        String name = DockerContextResolver.resolveCurrentContextName(
            dockerConfigDir,
            mapEnv("DOCKER_HOST", "tcp://1.2.3.4:2375", "DOCKER_CONTEXT", "orbstack")
        );

        assertThat(name).isNull();
    }

    @Test
    void blankDockerContextEnvIsIgnored(@TempDir Path dockerConfigDir) throws IOException {
        writeConfig(dockerConfigDir, "{\"currentContext\":\"desktop-linux\"}");

        String name = DockerContextResolver.resolveCurrentContextName(
            dockerConfigDir,
            mapEnv("DOCKER_CONTEXT", "  ")
        );

        assertThat(name).isEqualTo("desktop-linux");
    }

    @Test
    void resolvesDefaultWhenConfigHasNoCurrentContext(@TempDir Path dockerConfigDir) throws IOException {
        writeConfig(dockerConfigDir, "{\"auths\":{}}");

        String name = DockerContextResolver.resolveCurrentContextName(dockerConfigDir, k -> null);

        assertThat(name).isEqualTo(DockerContextResolver.DEFAULT_CONTEXT_NAME);
    }

    @Test
    void resolveEndpointReturnsNullForDefaultContext(@TempDir Path dockerConfigDir) {
        DockerContextResolver.DockerContextEndpoint endpoint = DockerContextResolver.resolveEndpoint(
            dockerConfigDir,
            DockerContextResolver.DEFAULT_CONTEXT_NAME
        );

        assertThat(endpoint).isNull();
    }

    @Test
    void resolveEndpointParsesHost(@TempDir Path dockerConfigDir) throws IOException {
        writeMeta(
            dockerConfigDir,
            "my-ctx",
            "{\"Name\":\"my-ctx\",\"Endpoints\":{\"docker\":{\"Host\":\"unix:///var/run/foo.sock\",\"SkipTLSVerify\":false}}}"
        );

        DockerContextResolver.DockerContextEndpoint endpoint = DockerContextResolver.resolveEndpoint(
            dockerConfigDir,
            "my-ctx"
        );

        assertThat(endpoint).isNotNull();
        assertThat(endpoint.getContextName()).isEqualTo("my-ctx");
        assertThat(endpoint.getHost().toString()).isEqualTo("unix:///var/run/foo.sock");
        assertThat(endpoint.isSkipTlsVerify()).isFalse();
        assertThat(endpoint.getTlsMaterialDir()).isNull();
    }

    @Test
    void resolveEndpointDiscoversTlsMaterial(@TempDir Path dockerConfigDir) throws IOException {
        writeMeta(
            dockerConfigDir,
            "remote",
            "{\"Name\":\"remote\",\"Endpoints\":{\"docker\":{\"Host\":\"tcp://10.0.0.5:2376\"}}}"
        );
        Path tlsDir = DockerContextResolver.contextTlsDir(dockerConfigDir, "remote");
        Files.createDirectories(tlsDir);
        Files.write(tlsDir.resolve("ca.pem"), "ca".getBytes(StandardCharsets.UTF_8));

        DockerContextResolver.DockerContextEndpoint endpoint = DockerContextResolver.resolveEndpoint(
            dockerConfigDir,
            "remote"
        );

        assertThat(endpoint.getTlsMaterialDir()).isEqualTo(tlsDir);
    }

    @Test
    void resolveEndpointThrowsWhenContextMissing(@TempDir Path dockerConfigDir) {
        assertThatThrownBy(() -> DockerContextResolver.resolveEndpoint(dockerConfigDir, "ghost"))
            .isInstanceOf(InvalidConfigurationException.class)
            .hasMessageContaining("ghost");
    }

    @Test
    void resolveEndpointThrowsWhenHostMissing(@TempDir Path dockerConfigDir) throws IOException {
        writeMeta(dockerConfigDir, "broken", "{\"Name\":\"broken\",\"Endpoints\":{\"docker\":{}}}");

        assertThatThrownBy(() -> DockerContextResolver.resolveEndpoint(dockerConfigDir, "broken"))
            .isInstanceOf(InvalidConfigurationException.class)
            .hasMessageContaining("docker endpoint host");
    }

    @Test
    void contextDirectoryNameMatchesDockerCliHash(@TempDir Path dockerConfigDir) {
        // Hashes captured from a real `~/.docker/contexts/meta/` directory created by Docker Desktop.
        assertThat(DockerContextResolver.contextMetaFile(dockerConfigDir, "desktop-linux"))
            .isEqualTo(
                dockerConfigDir
                    .resolve("contexts")
                    .resolve("meta")
                    .resolve(sha256("desktop-linux"))
                    .resolve("meta.json")
            );
        assertThat(sha256("desktop-linux"))
            .isEqualTo("fe9c6bd7a66301f49ca9b6a70b217107cd1284598bfc254700c989b916da791e");
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

    private static java.util.function.Function<String, String> mapEnv(String... pairs) {
        Map<String, String> map = new HashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            map.put(pairs[i], pairs[i + 1]);
        }
        return Collections.unmodifiableMap(map)::get;
    }

    private static String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
