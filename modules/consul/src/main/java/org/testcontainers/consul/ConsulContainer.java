package org.testcontainers.consul;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.Capability;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;


/**
 * GenericContainer subclass for Consul specific configuration and features. The main feature is the
 * withPropertyInConsul method, where users can specify keys and values to be pre-loaded into Consul for
 * their specific test scenario.
 * <p>
 */
public class ConsulContainer<SELF extends ConsulContainer<SELF>> extends GenericContainer<SELF> {

    private static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse("consul");
    private static final String DEFAULT_TAG = "1.10.4";

    private static final int CONSUL_HTTP_PORT = 8500;
    private static final int CONSUL_GRPC_PORT = 8502;

    private Map<String, String> propertiesMap = new HashMap<>();
    private List<String> initCommands = new ArrayList<>();

    private int httpPort = CONSUL_HTTP_PORT;
    private int grpcPort = CONSUL_GRPC_PORT;
    private String[] startConsulCmd = new String[]{"agent", "-dev", "-client", "0.0.0.0"};

    /**
     * @deprecated use {@link #ConsulContainer(DockerImageName)} instead
     */
    @Deprecated
    public ConsulContainer() {
        this(DEFAULT_IMAGE_NAME.withTag(DEFAULT_TAG));
    }

    public ConsulContainer(String dockerImageName) {
        this(DockerImageName.parse(dockerImageName));
    }

    public ConsulContainer(final DockerImageName dockerImageName) {
        super(dockerImageName);

        dockerImageName.assertCompatibleWith(DEFAULT_IMAGE_NAME);

        // Use the status leader endpoint to verify if consul is running.
        setWaitStrategy(Wait.forHttp("/v1/status/leader").forPort(httpPort).forStatusCode(200));

        withCreateContainerCmdModifier(cmd -> cmd.withCapAdd(Capability.IPC_LOCK));
        withEnv("CONSUL_ADDR", "http://0.0.0.0:" + httpPort);
        withCommand(startConsulCmd);
        withExposedPorts(httpPort, grpcPort);
    }

    @Override
    protected void containerIsStarted(InspectContainerResponse containerInfo) {
        addProperties();
        runInitCommands();
    }

    private void addProperties() {
        if (!propertiesMap.isEmpty()) {
            try {
                this.execInContainer(buildAddPropertyExecCommand(propertiesMap));
            } catch (IOException | InterruptedException e) {
                logger().error("Failed to add these properties {} into Consul via exec command. Exception message: {}", propertiesMap, e.getMessage());
            }
        }
    }

    private String[] buildAddPropertyExecCommand(Map<String, String> map) {
        StringBuilder stringBuilder = new StringBuilder();
        map.forEach((path, propertyValue) -> {
            stringBuilder.append(" && consul kv put " + path + " " + propertyValue);
        });
        return new String[]{"/bin/sh", "-c", stringBuilder.toString().substring(4)};
    }

    private void runInitCommands() {
        if (!initCommands.isEmpty()) {
            String commands = initCommands.stream()
                                    .map(command -> "consul " + command)
                                    .collect(Collectors.joining(" && "));
            try {
                ExecResult execResult = this.execInContainer(new String[]{"/bin/sh", "-c", commands});
                if (execResult.getExitCode() != 0) {
                    logger().error("Failed to execute these init commands {}. Exit code {}. Stdout {}. Stderr {}",
                                    initCommands, execResult.getExitCode(), execResult.getStdout(), execResult.getStderr());
                }
            } catch (IOException | InterruptedException e) {
                logger().error("Failed to execute these init commands {}. Exception message: {}", initCommands, e.getMessage());
            }
        }
    }

    /**
     * Pre-loads property into Consul container. User may specify one or more properties by chaining calls to this method.
     * <p>
     * The properties are added to Consul directly after the container is up via the
     * {@link #addProperties() addProperties}, called from {@link #containerIsStarted(InspectContainerResponse) containerIsStarted}
     *
     * @param path             specific Consul path to store specified property
     * @param propertyValue    property value for the given path
     * @return this
     */
    public SELF withPropertyInConsul(String path, String propertyValue) {
        propertiesMap.putIfAbsent(path, propertyValue);
        return self();
    }

    /**
     * Run initialization commands using the consul cli.
     * 
     * Useful for enableing more secret engines like:
     * <pre>
     *     .withInitCommand("secrets enable pki")
     *     .withInitCommand("secrets enable transit")
     * </pre>
     * @param commands The commands to send to the consul cli
     * @return this
     */
    public SELF withInitCommand(String... commands) {
        initCommands.addAll(Arrays.asList(commands));
        return self();
    }
}
