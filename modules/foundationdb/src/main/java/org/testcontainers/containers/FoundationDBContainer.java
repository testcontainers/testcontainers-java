package org.testcontainers.containers;

import com.github.dockerjava.api.command.ExecCreateCmdResponse;
import com.github.dockerjava.api.command.InspectContainerResponse;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.testcontainers.containers.output.FrameConsumerResultCallback;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.containers.output.ToStringConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.Base58;
import org.testcontainers.utility.DockerImageName;

import java.nio.charset.StandardCharsets;

/**
 * Constructs a single in memory <a href="https://www.foundationdb.org/">FoundationDB</a> database for testing
 * transactions.
 *
 * <p>Tested on a FoundationDB version 7.1.11 (that is the default version if not specified).</p>
 *
 * <p>Other docker images can be used from
 * <a href="https://hub.docker.com/r/foundationdb/foundationdb">foundationdb docker hub</a>. The FoundationDB
 * API version must be aligned with the docker version used (eg. for 7.1.11 use api version 711).</p>
 *
 * <p>FDB requires the native client libraries be installed separately from the java bindings. Install the libraries
 * before using the java FDB client. Also, it might have issues working on newer macOS with the java bindings, try using
 * java 8 and `export DYLD_LIBRARY_PATH=/usr/local/lib` in environment variables after installing FDB clients locally.
 * </p>
 */
@Slf4j
public class FoundationDBContainer extends GenericContainer<FoundationDBContainer> {

    private static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse("foundationdb/foundationdb");

    private static final String DEFAULT_TAG = "7.1.11";

    private static final int CONTAINER_EXIT_CODE_OK = 0;

    private static final int INTERNAL_PORT = 4500;

    private SocatContainer proxy;
    private int bindPort;

    private final String networkAlias;

    public FoundationDBContainer() {
        this(DEFAULT_IMAGE_NAME.withTag(DEFAULT_TAG));
    }

    @SneakyThrows
    public FoundationDBContainer(final DockerImageName dockerImageName) {
        super(dockerImageName);
        dockerImageName.assertCompatibleWith(DEFAULT_IMAGE_NAME);

        withCreateContainerCmdModifier(cmd ->
            cmd.withName(String.format("testcontainers-fdb-%s", Base58.randomString(8)))
        );

        withEnv("FDB_NETWORKING_MODE", "host");

        withNetwork(Network.newNetwork());
        networkAlias = "fdb-" + Base58.randomString(16);
        withNetworkAliases(networkAlias);

        this.waitStrategy = Wait.forLogMessage(".*FDBD joined cluster.*\\n", 1);
    }

    public String getConnectionString() {
        return String.format("docker:docker@%s:%d", getHost(), bindPort);
    }

    @SneakyThrows
    @Override
    public void doStart() {
        // FDB server port and the port seen by FDB clients must match, otherwise the FDB server will crash with
        // errors like "Assertion pkt.canonicalRemotePort == peerAddress.port failed..."
        proxy =
            new SocatContainer()
                .withNetwork(getNetwork())
                // the real port we want to proxy to FDB, will get the binding port after socat proxy container is starting
                .withExposedPorts(INTERNAL_PORT)
                // unused port, needed for the container to start before mapping the real port to the bind port of FDB
                // cannot use this directly as the mapped port will be known after proxy container starts
                .withTarget(INTERNAL_PORT + 1, networkAlias)
                .withReuse(this.isShouldBeReused());
        proxy.setWaitStrategy(null); // set so it does not wait for the check on the INTERNAL_PORT also
        proxy.start();
        // now socat proxy has started, will use the mapped port as the FDB port, so it will match with the
        // socat exposed port which is "seen" by FDB clients
        bindPort = proxy.getMappedPort(INTERNAL_PORT);
        // FDB server bind port can be set on env before starting the FDB container
        this.addEnv("FDB_PORT", String.valueOf(bindPort));

        // start FDB container, before proxying the bind port with socat
        super.doStart();

        // FDB container has started, so now proxy the real bind port
        proxyBindPort(INTERNAL_PORT, bindPort);
    }

    @SneakyThrows
    private void proxyBindPort(final int listenPort, final int mappedPort) {
        final ExecCreateCmdResponse createCmdResponse = dockerClient
            .execCreateCmd(proxy.getContainerId())
            .withCmd("socat", "TCP-LISTEN:" + listenPort + ",fork,reuseaddr", "TCP:" + networkAlias + ":" + mappedPort)
            .exec();

        final ToStringConsumer stdOutConsumer = new ToStringConsumer();
        final ToStringConsumer stdErrConsumer = new ToStringConsumer();

        try (FrameConsumerResultCallback callback = new FrameConsumerResultCallback()) {
            callback.addConsumer(OutputFrame.OutputType.STDOUT, stdOutConsumer);
            callback.addConsumer(OutputFrame.OutputType.STDERR, stdErrConsumer);
            dockerClient.execStartCmd(createCmdResponse.getId()).exec(callback);
        }
        final String stdOut = stdOutConsumer.toString(StandardCharsets.UTF_8);
        if (!stdOut.isEmpty()) {
            log.info("{}", stdOut);
        }
        final String stdErr = stdErrConsumer.toString(StandardCharsets.UTF_8);
        if (!stdErr.isEmpty()) {
            final String errorMessage = String.format("Error when attempting to bind port with socat: %s", stdErr);
            log.error("{}", errorMessage);
            throw new ProxyInitializationException(errorMessage);
        }
    }

    @Override
    protected void containerIsStarted(final InspectContainerResponse containerInfo) {
        // is faster when not checking if the database is initialized, and this is only necessary if reusing container
        if (isShouldBeReused()) {
            if (!isDatabaseInitialized()) {
                initDatabaseSingleInMemory();
            }
        } else {
            initDatabaseSingleInMemory();
        }
    }

    @SneakyThrows
    private boolean isDatabaseInitialized() {
        final String output = runCliExecOutput("status minimal");
        return output.contains("The database is available");
    }

    @SneakyThrows
    private void initDatabaseSingleInMemory() {
        log.debug("Initializing a single in memory database...");
        final String output = runCliExecOutput("configure new single memory");
        if (!output.contains("Database created")) {
            final String errorMessage = String.format(
                "Database not created when attempting to initialize a new single memory database. Output was: %s",
                output
            );
            log.error(errorMessage);
            throw new DatabaseInitializationException(errorMessage);
        }
        log.debug("Initialized successfully with a single in memory database.");
    }

    @SneakyThrows
    private String runCliExecOutput(final String command) {
        final ExecResult execResult = execInContainer("/usr/bin/fdbcli", "--exec", command);
        log.debug(execResult.getStdout());
        if (execResult.getExitCode() != CONTAINER_EXIT_CODE_OK) {
            final String errorMessage = String.format(
                "Exit code %s when attempting to run fdbcli command %s: %s",
                execResult.getExitCode(),
                command,
                execResult.getStdout()
            );
            log.error(errorMessage);
            throw new DatabaseInitializationException(errorMessage);
        }
        return execResult.getStdout();
    }

    public static class DatabaseInitializationException extends RuntimeException {

        DatabaseInitializationException(final String errorMessage) {
            super(errorMessage);
        }
    }

    public static class ProxyInitializationException extends RuntimeException {

        ProxyInitializationException(final String errorMessage) {
            super(errorMessage);
        }
    }
}
