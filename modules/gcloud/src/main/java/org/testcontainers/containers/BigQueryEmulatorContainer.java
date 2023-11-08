package org.testcontainers.containers;

import com.github.dockerjava.api.command.InspectContainerResponse;
import org.testcontainers.utility.DockerImageName;

import javax.annotation.Nullable;
import javax.net.ServerSocketFactory;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.Optional;
import java.util.Random;
import java.util.function.Supplier;

/**
 * Testcontainers implementation for BigQuery.
 * <p>
 * Supported image: {@code ghcr.io/goccy/bigquery-emulator}
 * <p>
 *
 * For further information on settings see
 * <a href="https://github.com/goccy/bigquery-emulator">https://github.com/goccy/bigquery-emulator</a>.
 *
 * The image <tt>ghcr.io/goccy/bigquery-emulator</tt> currently cannot be used with a docker host port
 * that differs from the container's Http and Grcp ports. By default, the container selects free random
 * Http and Grpc ports on the docker host system and sets them as the container's Http and Grpc ports.
 * See also <a href="https://github.com/goccy/bigquery-emulator/issues/160#issuecomment-1801515384">here</a>.
 */
public class BigQueryEmulatorContainer extends GenericContainer<BigQueryEmulatorContainer> {

    private static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse("ghcr.io/goccy/bigquery-emulator");

    private int httpPort = TestSocketUtils.findAvailableTcpPort();
    private int grpcPort = TestSocketUtils.findAvailableTcpPort();
    private String projectId = "test-project";
    private Optional<String> dataset = Optional.empty();
    private Optional<String> logLevel = Optional.empty();
    private Optional<String> logFormat = Optional.empty();
    private Optional<String> database = Optional.empty();
    private Optional<String> dataFromYaml = Optional.empty();
    private Optional<InitFunction> initFunction = Optional.empty();

    public BigQueryEmulatorContainer(String image) {
        this(DockerImageName.parse(image));
    }

    public BigQueryEmulatorContainer(DockerImageName dockerImageName) {
        super(dockerImageName);
        dockerImageName.assertCompatibleWith(DEFAULT_IMAGE_NAME);
    }

    @Override
    protected void configure() {
        StringBuilder command = new StringBuilder();
        command.append("bigquery-emulator --project=" + projectId + " --port=" + httpPort + " --grpc-port=" + grpcPort);
        dataset.ifPresent(it -> command.append(" --dataset=" + it));
        logLevel.ifPresent(it -> command.append(" --log-level=" + it));
        logFormat.ifPresent(it -> command.append(" --log-format=" + it));
        database.ifPresent(it -> command.append(" --database=" + it));
        dataFromYaml.ifPresent(it -> command.append(" --data-from-yaml=" + it));
        withCommand(command.toString());
        addFixedExposedPort(httpPort, httpPort);
        addFixedExposedPort(grpcPort, grpcPort);
    }

    public String getEmulatorHttpEndpoint() {
        return String.format("http://%s:%d", getHost(), httpPort);
    }

    public BigQueryEmulatorContainer withProjectId(String projectId) {
        this.projectId = projectId;
        return this;
    }
    public BigQueryEmulatorContainer withHttpPort(int httpPort) {
        this.httpPort = httpPort;
        return this;
    }
    public BigQueryEmulatorContainer withGrpcPort(int grpcPort) {
        this.grpcPort = grpcPort;
        return this;
    }

    public BigQueryEmulatorContainer withDataset(String dataset) {
        this.dataset = Optional.of(dataset);
        return this;
    }

    public BigQueryEmulatorContainer withLogLevel(String logLevel) {
        this.logLevel = Optional.of(logLevel);
        return this;
    }

    public BigQueryEmulatorContainer withLogFormat(String logFormat) {
        this.logFormat = Optional.of(logFormat);
        return this;
    }

    public BigQueryEmulatorContainer withDatabase(String database) {
        this.database = Optional.of(database);
        return this;
    }

    public BigQueryEmulatorContainer withDataFromYaml(String dataFromYaml) {
        this.dataFromYaml = Optional.of(dataFromYaml);
        return this;
    }

    /**
     * Function is called after the container has started.
     */
    public interface InitFunction {
        void init(BigQueryEmulatorContainer container, boolean reused);
    }

    /**
     * Perform tasks, such as populating BigQuery with schemas and data after the container
     * has started.
     */
    public BigQueryEmulatorContainer withInitFunction(InitFunction initFunction) {
        this.initFunction = Optional.of(initFunction);
        return this;
    }

    @Override
    protected void containerIsStarted(InspectContainerResponse containerInfo, boolean reused) {
        super.containerIsStarted(containerInfo, reused);
        initFunction.ifPresent(f -> f.init(this, reused));
    }

    public String getProjectId() {
        return projectId;
    }

    public int getHttpPort() {
        return httpPort;
    }

    public int getGrpcPort() {
        return grpcPort;
    }

    public Optional<String> getDataset() {
        return dataset;
    }

    public Optional<String> getLogLevel() {
        return logLevel;
    }

    public Optional<String> getLogFormat() {
        return logFormat;
    }

    public Optional<String> getDatabase() {
        return database;
    }

    public Optional<String> getDataFromYaml() {
        return dataFromYaml;
    }

    /**
     * Copied over from org.springframework.test.util.TestSocketUtils
     */
    private static class TestSocketUtils {

        /**
         * The minimum value for port ranges used when finding an available TCP port.
         */
        static final int PORT_RANGE_MIN = 1024;

        /**
         * The maximum value for port ranges used when finding an available TCP port.
         */
        static final int PORT_RANGE_MAX = 65535;

        private static final int PORT_RANGE_PLUS_ONE = PORT_RANGE_MAX - PORT_RANGE_MIN + 1;

        private static final int MAX_ATTEMPTS = 1_000;

        private static final Random random = new Random(System.nanoTime());

        private static final TestSocketUtils INSTANCE = new TestSocketUtils();


        /**
         * Although {@code TestSocketUtils} consists solely of static utility methods,
         * this constructor is intentionally {@code public}.
         * <h5>Rationale</h5>
         * <p>Static methods from this class may be invoked from within XML
         * configuration files using the Spring Expression Language (SpEL) and the
         * following syntax.
         * <pre><code>
         * &lt;bean id="myBean" ... p:port="#{T(org.springframework.test.util.TestSocketUtils).findAvailableTcpPort()}" /&gt;</code>
         * </pre>
         * <p>If this constructor were {@code private}, you would be required to supply
         * the fully qualified class name to SpEL's {@code T()} function for each usage.
         * Thus, the fact that this constructor is {@code public} allows you to reduce
         * boilerplate configuration with SpEL as can be seen in the following example.
         * <pre><code>
         * &lt;bean id="socketUtils" class="org.springframework.test.util.TestSocketUtils" /&gt;
         * &lt;bean id="myBean" ... p:port="#{socketUtils.findAvailableTcpPort()}" /&gt;</code>
         * </pre>
         */
        public TestSocketUtils() {
        }

        /**
         * Find an available TCP port randomly selected from the range [1024, 65535].
         *
         * @return an available TCP port number
         * @throws IllegalStateException if no available port could be found
         */
        public static int findAvailableTcpPort() {
            return INSTANCE.findAvailableTcpPortInternal();
        }


        /**
         * Internal implementation of {@link #findAvailableTcpPort()}.
         * <p>Package-private solely for testing purposes.
         */
        int findAvailableTcpPortInternal() {
            int candidatePort;
            int searchCounter = 0;
            do {
                state(++searchCounter <= MAX_ATTEMPTS, () -> String.format(
                    "Could not find an available TCP port in the range [%d, %d] after %d attempts",
                    PORT_RANGE_MIN, PORT_RANGE_MAX, MAX_ATTEMPTS));
                candidatePort = PORT_RANGE_MIN + random.nextInt(PORT_RANGE_PLUS_ONE);
            }
            while (!isPortAvailable(candidatePort));

            return candidatePort;
        }

        /**
         * Determine if the specified TCP port is currently available on {@code localhost}.
         * <p>Package-private solely for testing purposes.
         */
        boolean isPortAvailable(int port) {
            try {
                ServerSocket serverSocket = ServerSocketFactory.getDefault()
                    .createServerSocket(port, 1, InetAddress.getByName("localhost"));
                serverSocket.close();
                return true;
            } catch (Exception ex) {
                return false;
            }
        }

        private static void state(boolean expression, Supplier<String> messageSupplier) {
            if (!expression) {
                throw new IllegalStateException(nullSafeGet(messageSupplier));
            }
        }

        @Nullable
        private static String nullSafeGet(@Nullable Supplier<String> messageSupplier) {
            return (messageSupplier != null ? messageSupplier.get() : null);
        }


    }

}
