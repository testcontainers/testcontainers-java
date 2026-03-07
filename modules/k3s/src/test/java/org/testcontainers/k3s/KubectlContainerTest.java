package org.testcontainers.k3s;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.startupcheck.OneShotStartupCheckStrategy;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KubectlContainerTest {

    private static final Network network = Network.SHARED;

    private static final K3sContainer k3s = new K3sContainer(DockerImageName.parse("rancher/k3s:v1.21.3-k3s1"))
        .withNetwork(network)
        .withNetworkAliases("k3s");

    @BeforeAll
    static void setup() {
        k3s.start();
    }

    @AfterAll
    static void teardown() {
        k3s.stop();
    }

    @Test
    public void shouldExposeKubeConfigForNetworkAlias() throws Exception {
        String kubeConfigYaml = k3s.generateInternalKubeConfigYaml("k3s");

        try (
            GenericContainer<?> kubectlContainer = new GenericContainer<>("rancher/kubectl:v1.23.3")
                .withNetwork(network)
                .withCopyToContainer(Transferable.of(kubeConfigYaml), "/.kube/config")
                .withCommand("get namespaces")
                .withStartupCheckStrategy(new OneShotStartupCheckStrategy().withTimeout(Duration.ofSeconds(30)))
        ) {
            kubectlContainer.start();

            assertThat(kubectlContainer.getLogs()).contains("kube-system");
        }
    }

    @Test
    public void shouldThrowAnExceptionForUnknownNetworkAlias() {
        assertThatThrownBy(() -> k3s.generateInternalKubeConfigYaml("not-set-network-alias"))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
