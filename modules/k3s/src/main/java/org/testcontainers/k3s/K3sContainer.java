package org.testcontainers.k3s;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.github.dockerjava.api.command.InspectContainerResponse;
import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.SystemUtils;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Testcontainers implementation for K3S
 * <p>
 * Supported image: {@code rancher/k3s}
 */
public class K3sContainer extends GenericContainer<K3sContainer> {

    public static int KUBE_SECURE_PORT = 6443;

    public static int RANCHER_WEBHOOK_PORT = 8443;

    private String kubeConfigYaml;

    public K3sContainer(DockerImageName dockerImageName) {
        super(dockerImageName);
        dockerImageName.assertCompatibleWith(DockerImageName.parse("rancher/k3s"));

        addExposedPorts(KUBE_SECURE_PORT, RANCHER_WEBHOOK_PORT);
        setPrivilegedMode(true);
        withCreateContainerCmdModifier(it -> {
            it.getHostConfig().withCgroupnsMode("host");
        });
        addFileSystemBind("/sys/fs/cgroup", "/sys/fs/cgroup", BindMode.READ_WRITE);

        Map<String, String> tmpFsMapping = new HashMap<>();
        tmpFsMapping.put("/run", "");
        tmpFsMapping.put("/var/run", "");
        setTmpFsMapping(tmpFsMapping);

        List<String> commands = buildCommandArgs(dockerImageName, this.getHost(), isKmsgMissingOnLinux());
        setCommand(commands.toArray(new String[0]));
        setWaitStrategy(Wait.forLogMessage(".*Node controller sync successful.*", 1));
    }

    static List<String> buildCommandArgs(DockerImageName dockerImageName, String host, boolean isKmsgMissingOnLinux) {
        List<String> commands = new ArrayList<>();
        commands.add("server");
        commands.add("--disable=traefik");
        commands.add("--tls-san=" + host);
        // Allow kubelet to start in environments where /dev/kmsg is missing (e.g., isolated CI containers)
        if (isKmsgMissingOnLinux && supportsUserNamespaceFeatureGate(dockerImageName)) {
            commands.add("--kubelet-arg=--feature-gates=KubeletInUserNamespace=true");
        }
        return commands;
    }

    private static final java.util.regex.Pattern K3S_VERSION_PATTERN = java.util.regex.Pattern.compile(
        "^v?(\\d+)\\.(\\d+).*"
    );

    static boolean supportsUserNamespaceFeatureGate(DockerImageName dockerImageName) {
        if (dockerImageName.getDigest() != null) {
            return false;
        }
        String version = dockerImageName.getVersionPart();
        if ("latest".equals(version)) {
            return true;
        }
        java.util.regex.Matcher matcher = K3S_VERSION_PATTERN.matcher(version);
        if (!matcher.matches()) {
            return false;
        }
        int major = Integer.parseInt(matcher.group(1));
        int minor = Integer.parseInt(matcher.group(2));
        return major > 1 || (major == 1 && minor >= 22);
    }

    static boolean isKmsgMissingOnLinux() {
        return SystemUtils.IS_OS_LINUX && !new File("/dev/kmsg").exists();
    }

    @Override
    protected void containerIsStarted(InspectContainerResponse containerInfo) {
        String rawKubeConfig = copyFileFromContainer(
            "/etc/rancher/k3s/k3s.yaml",
            is -> IOUtils.toString(is, StandardCharsets.UTF_8)
        );
        String serverUrl = "https://" + this.getHost() + ":" + this.getMappedPort(KUBE_SECURE_PORT);
        kubeConfigYaml = kubeConfigWithServerUrl(rawKubeConfig, serverUrl);
    }

    /**
     * Return the kubernetes client configuration to access k3s from the host machine.
     *
     * @return the kubeConfig yaml.
     */
    public String getKubeConfigYaml() {
        return kubeConfigYaml;
    }

    /**
     * Generate a kubernetes client configuration for use on a docker internal network. The kubeConfig can be used by
     * another docker container running in the same network as the k3s container. For access from the host, use
     * the {@link #getKubeConfigYaml()} method instead.
     *
     * @param networkAlias a valid network alias of the k3s container.
     * @return the kubeConfig yaml.
     */
    public String generateInternalKubeConfigYaml(String networkAlias) {
        if (this.getNetworkAliases().contains(networkAlias)) {
            String serverUrl = "https://" + networkAlias + ":" + KUBE_SECURE_PORT;
            return kubeConfigWithServerUrl(kubeConfigYaml, serverUrl);
        } else {
            throw new IllegalArgumentException(networkAlias + " is not a network alias for k3s container");
        }
    }

    @SneakyThrows
    private String kubeConfigWithServerUrl(String kubeConfigYaml, String serverUrl) {
        ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());

        ObjectNode kubeConfigObjectNode = objectMapper.readValue(kubeConfigYaml, ObjectNode.class);

        JsonNode clusterNode = kubeConfigObjectNode.at("/clusters/0/cluster");
        if (!clusterNode.isObject()) {
            throw new IllegalStateException("'/clusters/0/cluster' expected to be an object");
        }
        ObjectNode clusterConfig = (ObjectNode) clusterNode;
        clusterConfig.replace("server", new TextNode(serverUrl));

        kubeConfigObjectNode.set("current-context", new TextNode("default"));

        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(kubeConfigObjectNode);
    }
}
