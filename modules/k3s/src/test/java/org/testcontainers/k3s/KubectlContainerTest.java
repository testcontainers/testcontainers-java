package org.testcontainers.k3s;

import org.assertj.core.api.Assertions;
import org.junit.ClassRule;
import org.junit.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.startupcheck.OneShotStartupCheckStrategy;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

public class KubectlContainerTest {

    public static Network network = Network.SHARED;

    @ClassRule
    public static K3sContainer k3s = new K3sContainer(DockerImageName.parse("rancher/k3s:v1.21.3-k3s1"))
        .withNetwork(network)
        .withNetworkAliases("k3s");

    @Test
    public void shouldExposeKubeConfigForNetworkAlias() throws Exception {

        String kubeConfigYaml = k3s.getKubeConfigYaml("k3s");

        Path tempFile = Files.createTempFile(null, null);
        Files.write(tempFile, kubeConfigYaml.getBytes(StandardCharsets.UTF_8));

        try (
            GenericContainer<?> kubectlContainer = new GenericContainer<>(DockerImageName.parse("rancher/kubectl:v1.23.3"))
                .withNetwork(network)
                .withCopyFileToContainer(MountableFile.forHostPath(tempFile.toAbsolutePath()), "/.kube/config")
                .withCommand("get namespaces")
                .withStartupCheckStrategy(
                    new OneShotStartupCheckStrategy().withTimeout(Duration.ofSeconds(30))
                )
        ) {
            kubectlContainer.start();

            String logs = kubectlContainer.getLogs();
            Assertions.assertThat(logs).contains("kube-system");
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowAnExceptionForUnknownNetworkAlias() {
        k3s.getKubeConfigYaml("not-set-network-alias");
    }
}
