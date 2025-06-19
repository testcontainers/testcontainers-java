package org.testcontainers.containers;

import org.junit.Test;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

public class DockerMcpGatewayContainerTest {

    @Test
    public void serviceSuccessfullyStarts() {
        try (DockerMcpGatewayContainer gateway = new DockerMcpGatewayContainer("docker/agents_gateway:v2")) {
            gateway.start();

            assertThat(gateway.isRunning()).isTrue();
        }
    }

    @Test
    public void gatewayStartsWithServers() {
        try (
            // container {
            DockerMcpGatewayContainer gateway = new DockerMcpGatewayContainer("docker/agents_gateway:v2")
                .withServer("curl", "curl")
                .withServer("brave", "brave_local_search", "brave_web_search")
                .withServer("github-official", Collections.singletonList("add_issue_comment"))
                .withSecret("brave.api_key", "test_key")
                .withSecrets(Collections.singletonMap("github.personal_access_token", "test_token"))
            // }
        ) {
            gateway.start();

            assertThat(gateway.getLogs()).contains("4 tools listed");
        }
    }
}
