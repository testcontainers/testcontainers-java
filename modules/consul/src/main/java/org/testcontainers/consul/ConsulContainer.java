package org.testcontainers.consul;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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

    private static final int CONSUL_HTTP_PORT = 8500;
    private static final int CONSUL_GRPC_PORT = 8502;

    private List<String> initCommands = new ArrayList<>();

    private int httpPort = CONSUL_HTTP_PORT;
    private int grpcPort = CONSUL_GRPC_PORT;
    private String[] startConsulCmd = new String[]{"agent", "-dev", "-client", "0.0.0.0"};

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
        runConsulCommands();
    }

    private void runConsulCommands() {
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
