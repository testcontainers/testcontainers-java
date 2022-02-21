package org.testcontainers.k3s;

import lombok.extern.slf4j.Slf4j;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.WaitingConsumer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import static org.testcontainers.containers.Network.newNetwork;
import static org.testcontainers.containers.output.OutputFrame.OutputType.STDOUT;

@Slf4j
public class KubectlContainerTest {

    public static Network network = newNetwork();

    public static K3sContainer k3s = new K3sContainer(DockerImageName.parse("rancher/k3s:v1.21.3-k3s1"))
        .withNetwork(network)
        .withNetworkAliases("k3s");

    @BeforeClass
    public static void setup() {
        k3s.start();
    }

    @AfterClass
    public static void tearDown() {
        k3s.stop();
    }

    @Test
    public void shouldExposeKubeConfigForNetworkAlias() throws Exception {

        String kubeConfigYaml = k3s.getKubeConfigYaml("k3s");

        Path tempFile = Files.createTempFile(null, null);
        Files.write(tempFile, kubeConfigYaml.getBytes(StandardCharsets.UTF_8));

        GenericContainer<?> kubectlContainer = new GenericContainer<>(DockerImageName.parse("rancher/kubectl:v1.23.3"))
            .withNetwork(network)
            .withCopyFileToContainer(MountableFile.forHostPath(tempFile.toAbsolutePath()), "/.kube/config")
            .withCommand("get namespaces");

        kubectlContainer.start();

        WaitingConsumer consumer = new WaitingConsumer();
        kubectlContainer.followOutput(consumer, STDOUT);

        consumer.waitUntil(frame ->
            frame.getUtf8String().contains("kube-system"), 30, TimeUnit.SECONDS);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowAnExceptionForUnknownNetworkAlias() {
        k3s.getKubeConfigYaml("not-set-network-alias");
    }
}
