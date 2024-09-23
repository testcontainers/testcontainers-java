package org.testcontainers.tibero;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import java.time.Duration;
import java.util.Collections;
import java.util.Set;

/**
 * Testcontainers implementation for Tibero Database.
 * <p>
 * Supported image: {@code ghcr.io/tibero-support/tibero7}
 * <p>
 * Exposed ports: 1521
 */
public class TiberoContainer extends JdbcDatabaseContainer<TiberoContainer> {

    static final String NAME = "tibero";

    private static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse("ghcr.io/tibero-support/tibero7");

    static final String IMAGE = DEFAULT_IMAGE_NAME.getUnversionedPart();

    static final int TIBERO_PORT = 8629;

    // this name should be same as the one in the license.xml

    private static final String CMD_HOST_NAME = "localhost";

    private static final String LICENSE_PATH = "./libs/license.xml";

    private static final String CONTAINER_LICENSE_PATH = "/tibero7/license/license.xml";

    private static final int DEFAULT_STARTUP_TIMEOUT_SECONDS = 180;

    // Container defaults
    static final String DEFAULT_DATABASE_NAME = "sys";

    static final String DEFAULT_SYS_USER = "sys";

    // Test container defaults
    static final String APP_USER = "tibero";

    static final String APP_USER_PASSWORD = "tibero";

    private String databaseName = DEFAULT_DATABASE_NAME;

    private String username = APP_USER;

    private String password = APP_USER_PASSWORD;

    public TiberoContainer(String dockerImageName, String licensePath) {
        this(DockerImageName.parse(dockerImageName), licensePath, CMD_HOST_NAME);
    }

    public TiberoContainer(String dockerImageName) {
        this(DockerImageName.parse(dockerImageName), LICENSE_PATH, CMD_HOST_NAME);
    }

    public TiberoContainer(final DockerImageName dockerImageName) {
        super(dockerImageName);
        dockerImageName.assertCompatibleWith(DEFAULT_IMAGE_NAME);

        // setting host name
        withCreateContainerCmdModifier(cmd -> cmd.withHostName(CMD_HOST_NAME));
        // setting license
        withCopyToContainer(MountableFile.forHostPath(LICENSE_PATH), CONTAINER_LICENSE_PATH);

        // setting tibero port
        addExposedPorts(TIBERO_PORT);

        waitingFor(
            Wait
                .forLogMessage(".*database system is ready to accept connections*\\s", 1)
                .withStartupTimeout(Duration.ofSeconds(DEFAULT_STARTUP_TIMEOUT_SECONDS))
        );
    }

    public TiberoContainer(final DockerImageName dockerImageName, String licensePath, String hostName) {
        super(dockerImageName);
        dockerImageName.assertCompatibleWith(DEFAULT_IMAGE_NAME);

        // setting host name
        withCreateContainerCmdModifier(cmd -> cmd.withHostName(hostName));
        // setting license
        withCopyToContainer(MountableFile.forHostPath(licensePath), CONTAINER_LICENSE_PATH);

        // setting tibero port
        addExposedPorts(TIBERO_PORT);

        waitingFor(
            Wait
                .forLogMessage(".*database system is ready to accept connections*\\s", 1)
                .withStartupTimeout(Duration.ofSeconds(DEFAULT_STARTUP_TIMEOUT_SECONDS))
        );
    }

    @Override
    protected void waitUntilContainerStarted() {
        getWaitStrategy().waitUntilReady(this);
    }

    @NotNull
    @Override
    public Set<Integer> getLivenessCheckPortNumbers() {
        return Collections.singleton(getMappedPort(TIBERO_PORT));
    }

    @Override
    public String getDriverClassName() {
        return "com.tmax.tibero.jdbc.TbDriver";
    }

    @Override
    public String getJdbcUrl() {
        return "jdbc:tibero:thin:" + "@" + getHost() + ":" + getTiberoPort() + ":" + getDatabaseName();
    }

    @Override
    public String getUsername() {
        // An application user is tied to the database, and therefore not authenticated to connect to SID.
        return username;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getDatabaseName() {
        return databaseName;
    }

    @Override
    public TiberoContainer withUsername(String username) {
        if (StringUtils.isEmpty(username)) {
            throw new IllegalArgumentException("Username cannot be null or empty");
        }
        if (DEFAULT_SYS_USER.equals(username)) {
            throw new IllegalArgumentException("Username cannot be " + DEFAULT_SYS_USER);
        }
        this.username = username;
        return self();
    }

    @Override
    public TiberoContainer withPassword(String password) {
        if (StringUtils.isEmpty(password)) {
            throw new IllegalArgumentException("Password cannot be null or empty");
        }
        this.password = password;
        return self();
    }

    @Override
    public TiberoContainer withDatabaseName(String databaseName) {
        if (StringUtils.isEmpty(databaseName)) {
            throw new IllegalArgumentException("Database name cannot be null or empty");
        }

        if (DEFAULT_DATABASE_NAME.equals(databaseName.toLowerCase())) {
            throw new IllegalArgumentException("Database name cannot be set to " + DEFAULT_DATABASE_NAME);
        }

        this.databaseName = databaseName;
        return self();
    }

    @Override
    public TiberoContainer withUrlParam(String paramName, String paramValue) {
        throw new UnsupportedOperationException("The Tibero Database driver does not support this");
    }

    public Integer getTiberoPort() {
        return getMappedPort(TIBERO_PORT);
    }

    @Override
    public String getTestQueryString() {
        return "SELECT 1 FROM DUAL";
    }

    @Override
    protected void configure() {
        withEnv("TIBERO_PASSWORD", password);

        // Only set TIBERO_DATABASE if different than the default.
        if (databaseName != DEFAULT_DATABASE_NAME) {
            withEnv("TIBERO_DATABASE", databaseName);
        }

        withEnv("APP_USER", username);
        withEnv("APP_USER_PASSWORD", password);
    }
}
