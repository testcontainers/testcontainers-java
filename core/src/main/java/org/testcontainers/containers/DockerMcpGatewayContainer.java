package org.testcontainers.containers;

import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.utility.DockerImageName;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Testcontainers implementation of the Docker MCP Gateway container.
 * <p>
 * Supported images: {@code docker/agents_gateway}
 * <p>
 * Exposed ports: 8811
 */
public class DockerMcpGatewayContainer extends GenericContainer<DockerMcpGatewayContainer> {

    private static final String DOCKER_AGENT_GATEWAY_IMAGE = "docker/agents_gateway";

    private static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse(DOCKER_AGENT_GATEWAY_IMAGE);

    private static final int DEFAULT_PORT = 8811;

    private static final String SECRETS_PATH = "/testcontainers/app/secrets";

    private final List<String> servers = new ArrayList<>();

    private final List<String> tools = new ArrayList<>();

    private final Map<String, String> secrets = new HashMap<>();

    public DockerMcpGatewayContainer(String dockerImageName) {
        this(DockerImageName.parse(dockerImageName));
    }

    public DockerMcpGatewayContainer(DockerImageName dockerImageName) {
        super(dockerImageName);
        dockerImageName.assertCompatibleWith(DEFAULT_IMAGE_NAME);
        withExposedPorts(DEFAULT_PORT);
        withFileSystemBind(DockerClientFactory.instance().getRemoteDockerUnixSocketPath(), "/var/run/docker.sock");
        waitingFor(Wait.forLogMessage(".*Start sse server on port.*", 1));
    }

    @Override
    protected void configure() {
        List<String> command = new ArrayList<>();
        command.add("--transport=sse");
        for (String server : this.servers) {
            if (!server.isEmpty()) {
                command.add("--servers=" + server);
            }
        }
        for (String tool : this.tools) {
            if (!tool.isEmpty()) {
                command.add("--tools=" + tool);
            }
        }
        if (this.secrets != null && !this.secrets.isEmpty()) {
            command.add("--secrets=" + SECRETS_PATH);
        }
        withCommand(String.join(" ", command));
    }

    @Override
    protected void containerIsCreated(String containerId) {
        if (this.secrets != null && !this.secrets.isEmpty()) {
            StringBuilder secretsFile = new StringBuilder();
            for (Map.Entry<String, String> entry : this.secrets.entrySet()) {
                secretsFile.append(entry.getKey()).append("=").append(entry.getValue()).append("\n");
            }
            copyFileToContainer(Transferable.of(secretsFile.toString()), SECRETS_PATH);
        }
    }

    public DockerMcpGatewayContainer withServer(String server, List<String> tools) {
        this.servers.add(server);
        this.tools.addAll(tools);
        return this;
    }

    public DockerMcpGatewayContainer withServer(String server, String... tools) {
        this.servers.add(server);
        this.tools.addAll(Arrays.asList(tools));
        return this;
    }

    public DockerMcpGatewayContainer withSecrets(Map<String, String> secrets) {
        this.secrets.putAll(secrets);
        return this;
    }

    public DockerMcpGatewayContainer withSecret(String secretKey, String secretValue) {
        this.secrets.put(secretKey, secretValue);
        return this;
    }

    public String getEndpoint() {
        return "http://" + getHost() + ":" + getMappedPort(DEFAULT_PORT);
    }
}
