package org.testcontainers.k3s;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class K3sContainerTest {

    @Test
    void shouldNotIncludeUserNamespaceFeatureGateByDefault() {
        List<String> args = K3sContainer.buildCommandArgs("localhost", false);

        assertThat(args).containsExactly("server", "--disable=traefik", "--tls-san=localhost");
    }

    @Test
    void shouldIncludeUserNamespaceFeatureGateWhenKmsgMissingOnLinux() {
        List<String> args = K3sContainer.buildCommandArgs("localhost", true);

        assertThat(args)
            .containsExactly(
                "server",
                "--disable=traefik",
                "--tls-san=localhost",
                "--kubelet-arg=--feature-gates=KubeletInUserNamespace=true"
            );
    }
}
