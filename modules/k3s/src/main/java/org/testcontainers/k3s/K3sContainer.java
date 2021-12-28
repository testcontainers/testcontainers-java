package org.testcontainers.k3s;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.github.dockerjava.api.command.InspectContainerResponse;
import lombok.SneakyThrows;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;
import org.testcontainers.utility.DockerImageName;

import java.util.HashMap;
import java.util.Map;

public class K3sContainer extends GenericContainer<K3sContainer> {

    private String kubeConfigYaml;

    private static final String TOKEN = "fixedtoken";

    public K3sContainer(DockerImageName dockerImageName) {
        super(dockerImageName);
        dockerImageName.assertCompatibleWith(DockerImageName.parse("rancher/k3s"));

        addExposedPorts(6443, 8443);
        setPrivilegedMode(true);

        Map<String, String> tmpFsMapping = new HashMap<>();
        tmpFsMapping.put("/run", "");
        tmpFsMapping.put("/var/run", "");
        setTmpFsMapping(tmpFsMapping);

        setCommand(
            "server",
            "--no-deploy=traefik",
            "--token=" + TOKEN,
            "--tls-san=" + this.getHost()
        );
        setWaitStrategy(new LogMessageWaitStrategy().withRegEx(".*Node controller sync successful.*"));
    }

    @SneakyThrows
    @Override
    protected void containerIsStarted(InspectContainerResponse containerInfo) {
        ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());

        JsonNode rawKubeConfig = copyFileFromContainer(
            "/etc/rancher/k3s/k3s.yaml",
            is -> objectMapper.readValue(is, JsonNode.class)
        );

        ObjectNode clusterConfig = (ObjectNode) rawKubeConfig.get("clusters").get(0).get("cluster");
        clusterConfig.replace("server", new TextNode("https://" + this.getHost() + ":" + this.getMappedPort(6443)));

        ((ObjectNode) rawKubeConfig).set("current-context", new TextNode("default"));

        kubeConfigYaml = objectMapper.writeValueAsString(rawKubeConfig);
    }

    public String getKubeConfigYaml() {
        return kubeConfigYaml;
    }
}
