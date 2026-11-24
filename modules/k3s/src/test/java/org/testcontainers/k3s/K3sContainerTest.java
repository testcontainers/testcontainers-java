package org.testcontainers.k3s;

import org.junit.jupiter.api.Test;
import org.testcontainers.utility.DockerImageName;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class K3sContainerTest {

    private static final DockerImageName SUPPORTED_IMAGE = DockerImageName.parse("rancher/k3s:v1.22.0-k3s1");
    private static final DockerImageName LEGACY_IMAGE = DockerImageName.parse("rancher/k3s:v1.21.3-k3s1");

    @Test
    void shouldNotIncludeUserNamespaceFeatureGateByDefault() {
        List<String> args = K3sContainer.buildCommandArgs(SUPPORTED_IMAGE, "localhost", false);

        assertThat(args).containsExactly("server", "--disable=traefik", "--tls-san=localhost");
    }

    @Test
    void shouldIncludeUserNamespaceFeatureGateWhenKmsgMissingOnLinuxForSupportedVersion() {
        List<String> args = K3sContainer.buildCommandArgs(SUPPORTED_IMAGE, "localhost", true);

        assertThat(args)
            .containsExactly(
                "server",
                "--disable=traefik",
                "--tls-san=localhost",
                "--kubelet-arg=--feature-gates=KubeletInUserNamespace=true"
            );
    }

    @Test
    void shouldNotIncludeUserNamespaceFeatureGateForLegacyVersionEvenWhenKmsgMissing() {
        List<String> args = K3sContainer.buildCommandArgs(LEGACY_IMAGE, "localhost", true);

        assertThat(args).containsExactly("server", "--disable=traefik", "--tls-san=localhost");
    }

    @Test
    void shouldNotIncludeUserNamespaceFeatureGateForOpaqueTag() {
        DockerImageName opaqueImage = DockerImageName.parse("rancher/k3s:legacy");
        List<String> args = K3sContainer.buildCommandArgs(opaqueImage, "localhost", true);

        assertThat(args).containsExactly("server", "--disable=traefik", "--tls-san=localhost");
    }

    @Test
    void shouldNotIncludeUserNamespaceFeatureGateForDigestReference() {
        DockerImageName digestImage = DockerImageName.parse(
            "rancher/k3s@sha256:0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"
        );
        List<String> args = K3sContainer.buildCommandArgs(digestImage, "localhost", true);

        assertThat(args).containsExactly("server", "--disable=traefik", "--tls-san=localhost");
    }
}
