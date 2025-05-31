package org.testcontainers.k3s;

import org.junit.ClassRule;
import org.junit.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.startupcheck.OneShotStartupCheckStrategy;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.junit4.TestcontainersRule;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

public class KubectlContainerTest {

    public static Network network = Network.SHARED;

    @ClassRule
    public static TestcontainersRule<K3sContainer> k3s = new TestcontainersRule<>(
        new K3sContainer(DockerImageName.parse("rancher/k3s:v1.21.3-k3s1"))
            .withNetwork(network)
            .withNetworkAliases("k3s")
    );

    @Test
    public void shouldExposeKubeConfigForNetworkAlias() throws Exception {
        String kubeConfigYaml = k3s.get().generateInternalKubeConfigYaml("k3s");

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

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowAnExceptionForUnknownNetworkAlias() {
        k3s.get().generateInternalKubeConfigYaml("not-set-network-alias");
    }
}
