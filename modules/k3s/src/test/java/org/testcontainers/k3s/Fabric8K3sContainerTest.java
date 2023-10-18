package org.testcontainers.k3s;

import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.ContainerPortBuilder;
import io.fabric8.kubernetes.api.model.Node;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.api.model.PodSpec;
import io.fabric8.kubernetes.api.model.PodSpecBuilder;
import io.fabric8.kubernetes.api.model.ProbeBuilder;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.dsl.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.utility.DockerImageName;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
public class Fabric8K3sContainerTest {

    @Test
    public void shouldStartAndHaveListableNode() {
        runK3s(DockerImageName.parse("rancher/k3s:v1.21.3-k3s1"));
    }

    @Test
    public void shouldStartAndHaveListableNode_backwardsCompat() {
        runK3s(DockerImageName.parse("rancher/k3s:v1.17.17-k3s1"));
    }

    private void runK3s(DockerImageName k3sDockerImage) {
        try (
            // starting_k3s {
            K3sContainer k3s = new K3sContainer(k3sDockerImage)
                .withLogConsumer(new Slf4jLogConsumer(log))
            // }
        ) {
            k3s.start();

            // connecting_with_fabric8 {
            // obtain a kubeconfig file which allows us to connect to k3s
            String kubeConfigYaml = k3s.getKubeConfigYaml();

            // requires io.fabric8:kubernetes-client:5.11.0 or higher
            Config config = Config.fromKubeconfig(kubeConfigYaml);

            DefaultKubernetesClient client = new DefaultKubernetesClient(config);

            // interact with the running K3s server, e.g.:
            List<Node> nodes = client.nodes().list().getItems();
            // }

            assertThat(nodes).hasSize(1);

            // verify that we can start a pod
            Pod helloworld = dummyStartablePod();
            client.pods().create(helloworld);
            client.pods().inNamespace("default").withName("helloworld").waitUntilReady(30, TimeUnit.SECONDS);

            assertThat(client.pods().inNamespace("default").withName("helloworld"))
                .extracting(Resource::isReady)
                .isEqualTo(true);
        }
    }

    private Pod dummyStartablePod() {
        PodSpec podSpec = new PodSpecBuilder()
            .withContainers(
                new ContainerBuilder()
                    .withName("helloworld")
                    .withImage("testcontainers/helloworld:1.1.0")
                    .withPorts(new ContainerPortBuilder().withContainerPort(8080).build())
                    .withReadinessProbe(new ProbeBuilder().withNewTcpSocket().withNewPort(8080).endTcpSocket().build())
                    .build()
            )
            .build();

        return new PodBuilder()
            .withNewMetadata()
            .withName("helloworld")
            .withNamespace("default")
            .endMetadata()
            .withSpec(podSpec)
            .build();
    }
}
