package org.testcontainers.k3s;

import io.fabric8.kubernetes.api.model.Node;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.util.List;

@Slf4j
public class Fabric8K3sContainerTest {

    @Test
    public void shouldStartAndHaveListableNode() throws IOException {
        try (
            // starting_k3s {
            K3sContainer k3s = new K3sContainer(DockerImageName.parse("rancher/k3s:v1.21.3-k3s1"))
                .withLogConsumer(new Slf4jLogConsumer(log))
            // }
        ) {
            k3s.start();

            // connecting_with_fabric8 {
            String kubeConfigYaml = k3s.getKubeConfigYaml();

            Config config = Config.fromKubeconfig(kubeConfigYaml);
            // workaround for undiagnosed issue; fabric8 seems to not identify
            // the client key algorithm correctly, so fails to work with K3s
            // ECDSA keys unless configured explicitly
            config.setClientKeyAlgo("EC");

            DefaultKubernetesClient client = new DefaultKubernetesClient(config);

            // interact with the running K3s server, e.g.:
            List<Node> nodes = client.nodes().list().getItems();
            // }

            Assertions.assertThat(nodes).hasSize(1);
        }
    }
}
