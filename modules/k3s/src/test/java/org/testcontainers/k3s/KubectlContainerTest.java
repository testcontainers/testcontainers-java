package org.testcontainers.k3s;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.startupcheck.OneShotStartupCheckStrategy;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.junit4.ClassContainer;
import org.testcontainers.junit4.TestContainersRunner;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(TestContainersRunner.class)
public class KubectlContainerTest {

    public static Network network = Network.SHARED;

    @ClassContainer
    public static K3sContainer k3s = new K3sContainer(DockerImageName.parse("rancher/k3s:v1.21.3-k3s1"))
        .withNetwork(network)
        .withNetworkAliases("k3s");

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

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowAnExceptionForUnknownNetworkAlias() {
        k3s.generateInternalKubeConfigYaml("not-set-network-alias");
    }
}
