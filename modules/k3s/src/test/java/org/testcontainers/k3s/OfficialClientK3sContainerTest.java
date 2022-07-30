package org.testcontainers.k3s;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1NodeList;
import io.kubernetes.client.util.Config;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.io.StringReader;

@Slf4j
public class OfficialClientK3sContainerTest {

    @Test
    public void shouldStartAndHaveListableNode() throws IOException, ApiException {
        try (
            // starting_k3s {
            K3sContainer k3s = new K3sContainer(DockerImageName.parse("rancher/k3s:v1.21.3-k3s1"))
                .withLogConsumer(new Slf4jLogConsumer(log))
            // }
        ) {
            k3s.start();

            // connecting_with_k8sio {
            String kubeConfigYaml = k3s.getKubeConfigYaml();

            ApiClient client = Config.fromConfig(new StringReader(kubeConfigYaml));
            CoreV1Api api = new CoreV1Api(client);

            // interact with the running K3s server, e.g.:
            V1NodeList nodes = api.listNode(null, null, null, null, null, null, null, null, null, null);
            // }

            Assertions.assertThat(nodes.getItems()).hasSize(1);
        }
    }
}
