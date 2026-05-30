package org.testcontainers.doris;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * Testcontainers implementation for Apache Doris.
 * <p>
 * Supported image: {@code apache/doris}
 * <p>
 * Version tags such as {@code 3.1.0} start a FE container and a managed BE container using
 * {@code apache/doris:fe-3.1.0} and {@code apache/doris:be-3.1.0}. All-in-one tags such as
 * {@code doris-all-in-one-2.1.0} and {@code 3.0.5-all} are also supported.
 * <p>
 * Exposed ports:
 * <ul>
 *     <li>FE MySQL protocol: 9030</li>
 *     <li>FE HTTP: 8030</li>
 * </ul>
 */
public class DorisContainer extends JdbcDatabaseContainer<DorisContainer> {

    static final String NAME = "doris";

    static final DockerImageName DOCKER_IMAGE_NAME = DockerImageName.parse("apache/doris");

    private static final String FE_TAG_PREFIX = "fe-";

    private static final String BE_TAG_PREFIX = "be-";

    private static final String ALL_IN_ONE_TAG_PREFIX = "doris-all-in-one-";

    private static final String ALL_IN_ONE_TAG_SUFFIX = "-all";

    private static final String DORIS_NETWORK_SUBNET = "172.20.80.0/24";

    private static final String DORIS_NETWORK_GATEWAY = "172.20.80.1";

    private static final String FE_ADDRESS = "172.20.80.2";

    private static final String BE_ADDRESS = "172.20.80.3";

    private static final String FE_NETWORK_ALIAS = "doris-fe";

    private static final String BE_NETWORK_ALIAS = "doris-be";

    private static final Integer EDIT_LOG_PORT = 9010;

    private static final Integer QUERY_PORT = 9030;

    private static final Integer HTTP_PORT = 8030;

    private static final Integer BE_HEARTBEAT_PORT = 9050;

    private static final Integer BE_HTTP_PORT = 8040;

    private static final String FE_SERVERS = "fe1:" + FE_ADDRESS + ":" + EDIT_LOG_PORT;

    private static final String FE_STARTUP_COMMAND =
        "sed -i 's/-Xmx8192m -Xms8192m/-Xmx512m -Xms512m/g; s/-Xmx8192m/-Xmx512m/g' " +
        "/opt/apache-doris/fe/conf/fe.conf && exec bash /usr/local/bin/init_fe.sh";

    private static final String BE_STARTUP_COMMAND =
        "sed -i 's/-Xmx2048m/-Xmx256m/g' /opt/apache-doris/be/conf/be.conf && " +
        "mkdir -p /opt/apache-doris/be/storage && " +
        "printf '\\nstorage_root_path = /opt/apache-doris/be/storage\\n' >> /opt/apache-doris/be/conf/be.conf && " +
        "printf '\\nmem_limit = 1g\\n' >> /opt/apache-doris/be/conf/be.conf && " +
        "exec bash /usr/local/bin/entry_point.sh";

    private static final String DRIVER_CLASS_NAME = "com.mysql.cj.jdbc.Driver";

    private static final String LEGACY_DRIVER_CLASS_NAME = "com.mysql.jdbc.Driver";

    private static final String DEFAULT_DATABASE_NAME = "test";

    private static final String DEFAULT_USERNAME = "root";

    private static final String DEFAULT_PASSWORD = "";

    private static final String BACKEND_READINESS_TABLE_NAME = "testcontainers_backend_ready";

    private String databaseName = DEFAULT_DATABASE_NAME;

    private String username = DEFAULT_USERNAME;

    private String password = DEFAULT_PASSWORD;

    private DockerImageName backendImageName;

    private GenericContainer<?> backendContainer;

    private Network network;

    private boolean createdNetwork;

    public DorisContainer(String dockerImageName) {
        this(DockerImageName.parse(dockerImageName));
    }

    public DorisContainer(final DockerImageName dockerImageName) {
        this(dockerImageName, resolveFrontendImageName(dockerImageName));
    }

    private DorisContainer(final DockerImageName dockerImageName, final DockerImageName frontendImageName) {
        super(frontendImageName);
        frontendImageName.assertCompatibleWith(DOCKER_IMAGE_NAME);

        addExposedPorts(QUERY_PORT, HTTP_PORT);
        this.backendImageName = resolveBackendImageName(dockerImageName);
        withStartupTimeoutSeconds(240);
    }

    @Override
    protected void configure() {
        if (isSplitMode()) {
            configureSplitMode();
        }
    }

    @Override
    protected void doStart() {
        try {
            super.doStart();
        } catch (RuntimeException e) {
            closeCreatedNetwork();
            throw e;
        }
    }

    private void configureSplitMode() {
        if (network == null) {
            network = createNetwork();
            createdNetwork = true;
        }

        super.withNetwork(network);
        super.withNetworkAliases(FE_NETWORK_ALIAS);
        withCreateContainerCmdModifier(cmd ->
            cmd.withIpv4Address(FE_ADDRESS).withEntrypoint("bash", "-c").withCmd(FE_STARTUP_COMMAND)
        );
        addEnv("FE_SERVERS", FE_SERVERS);
        addEnv("FE_ID", "1");
    }

    private Network createNetwork() {
        return Network
            .builder()
            .createNetworkCmdModifier(cmd ->
                cmd.withIpam(
                    new com.github.dockerjava.api.model.Network.Ipam()
                        .withConfig(
                            new com.github.dockerjava.api.model.Network.Ipam.Config()
                                .withSubnet(DORIS_NETWORK_SUBNET)
                                .withGateway(DORIS_NETWORK_GATEWAY)
                        )
                )
            )
            .build();
    }

    @Override
    public String getDriverClassName() {
        try {
            Class.forName(DRIVER_CLASS_NAME);
            return DRIVER_CLASS_NAME;
        } catch (ClassNotFoundException e) {
            return LEGACY_DRIVER_CLASS_NAME;
        }
    }

    @Override
    public String getJdbcUrl() {
        return constructDorisJdbcUrl(databaseName);
    }

    public String getHttpUrl() {
        return "http://" + getHost() + ":" + getMappedPort(HTTP_PORT);
    }

    @Override
    protected String constructUrlForConnection(String queryString) {
        return appendDefaultUrlParameters(super.constructUrlForConnection(queryString));
    }

    @Override
    public DorisContainer withNetwork(Network network) {
        this.network = network;
        this.createdNetwork = false;
        return super.withNetwork(network);
    }

    public DorisContainer withBackendImage(String dockerImageName) {
        return withBackendImage(DockerImageName.parse(dockerImageName));
    }

    public DorisContainer withBackendImage(DockerImageName backendImageName) {
        backendImageName.assertCompatibleWith(DOCKER_IMAGE_NAME);
        this.backendImageName = backendImageName;
        return self();
    }

    @SuppressWarnings("deprecation")
    @Override
    protected void waitUntilContainerStarted() {
        logger()
            .info(
                "Waiting for Doris database connection to become available at {} using query '{}'",
                getJdbcUrl(),
                getTestQueryString()
            );

        long start = System.nanoTime();
        Exception lastConnectionException = null;
        while ((System.nanoTime() - start) < TimeUnit.SECONDS.toNanos(getStartupTimeoutSeconds())) {
            if (!isRunning()) {
                sleep();
                continue;
            }

            if (isSplitMode()) {
                startBackendContainer();
            }

            try (
                Connection connection = createDorisConnection(null);
                Statement statement = connection.createStatement()
            ) {
                statement.execute(getTestQueryString());
                if (isSplitMode() && !hasAliveBackend(statement)) {
                    throw new SQLException("Doris BE is not alive yet");
                }
                statement.execute("CREATE DATABASE IF NOT EXISTS " + quoteIdentifier(databaseName));
                if (isSplitMode()) {
                    verifyBackendCanCreateReplicas(statement);
                }
                return;
            } catch (NoDriverFoundException e) {
                throw e;
            } catch (Exception e) {
                lastConnectionException = e;
                logger().debug("Failure when trying test query", e);
                sleep();
            }
        }

        throw new IllegalStateException(
            String.format(
                "Container is started, but cannot be accessed by (JDBC URL: %s), please check container logs",
                this.getJdbcUrl()
            ),
            lastConnectionException
        );
    }

    private void startBackendContainer() {
        if (backendContainer == null) {
            backendContainer =
                new GenericContainer<>(backendImageName)
                    .withNetwork(network)
                    .withNetworkAliases(BE_NETWORK_ALIAS)
                    .withExposedPorts(BE_HTTP_PORT, BE_HEARTBEAT_PORT)
                    .withCreateContainerCmdModifier(cmd ->
                        cmd.withIpv4Address(BE_ADDRESS).withEntrypoint("bash", "-c").withCmd(BE_STARTUP_COMMAND)
                    )
                    .withEnv("FE_SERVERS", FE_SERVERS)
                    .withEnv("BE_ADDR", BE_ADDRESS + ":" + BE_HEARTBEAT_PORT)
                    .withEnv("MASTER_FE_IP", FE_ADDRESS)
                    .withEnv("CURRENT_BE_IP", BE_ADDRESS)
                    .withEnv("CURRENT_BE_PORT", String.valueOf(BE_HEARTBEAT_PORT))
                    .withEnv("PRIORITY_NETWORKS", DORIS_NETWORK_SUBNET)
                    .waitingFor(Wait.forListeningPort().withStartupTimeout(Duration.ofMinutes(4)));
        }

        if (!backendContainer.isRunning()) {
            backendContainer.start();
        }
    }

    private boolean hasAliveBackend(Statement statement) throws SQLException {
        try (ResultSet resultSet = statement.executeQuery("SHOW BACKENDS")) {
            while (resultSet.next()) {
                if (Boolean.parseBoolean(resultSet.getString("Alive"))) {
                    return true;
                }
            }
        }
        return false;
    }

    private void verifyBackendCanCreateReplicas(Statement statement) throws SQLException {
        String readinessTableName = quoteIdentifier(databaseName) + "." + quoteIdentifier(BACKEND_READINESS_TABLE_NAME);
        statement.execute(
            "CREATE TABLE IF NOT EXISTS " +
            readinessTableName +
            " (id INT) DUPLICATE KEY(id) DISTRIBUTED BY HASH(id) BUCKETS 1 PROPERTIES (\"replication_num\" = \"1\")"
        );
        statement.execute("INSERT INTO " + readinessTableName + " VALUES (1)");
        statement.execute("DROP TABLE IF EXISTS " + readinessTableName);
    }

    private Connection createDorisConnection(String databaseName) throws SQLException {
        Properties properties = new Properties();
        properties.put("user", getUsername());
        properties.put("password", getPassword());

        Connection connection = getJdbcDriverInstance()
            .connect(appendDefaultUrlParameters(constructDorisJdbcUrl(databaseName)), properties);
        if (connection == null) {
            throw new SQLException("Doris JDBC driver returned a null connection");
        }
        return connection;
    }

    private String constructDorisJdbcUrl(String databaseName) {
        String additionalUrlParams = constructUrlParameters("?", "&");
        String databasePath = databaseName == null ? "" : databaseName;
        return "jdbc:mysql://" + getHost() + ":" + getMappedPort(QUERY_PORT) + "/" + databasePath + additionalUrlParams;
    }

    private String appendDefaultUrlParameters(String url) {
        url = appendUrlParameterIfAbsent(url, "useSSL", "false");
        return appendUrlParameterIfAbsent(url, "allowPublicKeyRetrieval", "true");
    }

    private String appendUrlParameterIfAbsent(String url, String name, String value) {
        if (url.contains(name + "=")) {
            return url;
        }

        String separator = url.contains("?") ? "&" : "?";
        return url + separator + name + "=" + value;
    }

    private String quoteIdentifier(String identifier) {
        return "`" + identifier.replace("`", "``") + "`";
    }

    private void sleep() {
        try {
            Thread.sleep(100L);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for Doris to start", e);
        }
    }

    private boolean isSplitMode() {
        return backendImageName != null;
    }

    private static DockerImageName resolveFrontendImageName(DockerImageName dockerImageName) {
        String tag = dockerImageName.getVersionPart();

        if (isBackendTag(tag)) {
            throw new IllegalArgumentException(
                "DorisContainer must be created with a FE image, version tag, or all-in-one image"
            );
        }

        if (isFrontendTag(tag) || isAllInOneTag(tag)) {
            return dockerImageName;
        }

        return dockerImageName.withTag(FE_TAG_PREFIX + tag);
    }

    private static DockerImageName resolveBackendImageName(DockerImageName dockerImageName) {
        String tag = dockerImageName.getVersionPart();

        if (isAllInOneTag(tag)) {
            return null;
        }

        if (isFrontendTag(tag)) {
            return dockerImageName.withTag(BE_TAG_PREFIX + tag.substring(FE_TAG_PREFIX.length()));
        }

        return dockerImageName.withTag(BE_TAG_PREFIX + tag);
    }

    private static boolean isFrontendTag(String tag) {
        return tag.startsWith(FE_TAG_PREFIX);
    }

    private static boolean isBackendTag(String tag) {
        return tag.startsWith(BE_TAG_PREFIX);
    }

    private static boolean isAllInOneTag(String tag) {
        return tag.startsWith(ALL_IN_ONE_TAG_PREFIX) || tag.endsWith(ALL_IN_ONE_TAG_SUFFIX);
    }

    @Override
    public void stop() {
        try {
            if (backendContainer != null) {
                backendContainer.stop();
                backendContainer = null;
            }
        } finally {
            try {
                super.stop();
            } finally {
                closeCreatedNetwork();
            }
        }
    }

    private void closeCreatedNetwork() {
        if (createdNetwork && network != null) {
            network.close();
            network = null;
            createdNetwork = false;
        }
    }

    @Override
    public String getDatabaseName() {
        return databaseName;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getTestQueryString() {
        return "SELECT 1";
    }

    @Override
    public DorisContainer withDatabaseName(String databaseName) {
        this.databaseName = databaseName;
        return self();
    }

    @Override
    public DorisContainer withUsername(String username) {
        this.username = username;
        return self();
    }

    @Override
    public DorisContainer withPassword(String password) {
        this.password = password;
        return self();
    }
}
