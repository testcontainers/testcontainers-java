package org.testcontainers.containers;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;

/**
 * Testcontainers implementation for Oracle.
 * <p>
 * Supported image: {@code container-registry.oracle.com/database/free}
 * <p>
 * Exposed ports: 1521
 * <p>
 * MIT-licensesd by Bernd Eckenfels, based on OracleContainer from oracle-xe.
 */
public class OracleFreeContainer extends JdbcDatabaseContainer<OracleFreeContainer> {

    public static final String NAME = "oracle";

    private static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse("container-registry.oracle.com/database/free");

    // https://container-registry.oracle.com/ords/f?p=113:4:16246212162327:::RP,4:P4_REPOSITORY,AI_REPOSITORY,P4_REPOSITORY_NAME,AI_REPOSITORY_NAME:1863,1863,Oracle%20Database%20Free,Oracle%20Database%20Free&cs=3gQONqcVhKMn1mUNRMJjdHgofFIzDG97WTyRx3tHBfkK8axlWmSmJRNF_YnrAizYZZj5kyAnyS9O_nudXM3Xw6w
    static final String DEFAULT_TAG = "23.3.0.0";

    static final String IMAGE = DEFAULT_IMAGE_NAME.getUnversionedPart();

    static final int ORACLE_PORT = 1521;

    private static final int DEFAULT_STARTUP_TIMEOUT_SECONDS = 240;

    private static final int DEFAULT_CONNECT_TIMEOUT_SECONDS = 120;

    // Container defaults
    static final String DEFAULT_DATABASE_NAME = "freepdb1";

    static final String DEFAULT_SID = "free";

    static final String DEFAULT_SYSTEM_USER = "system";

    static final String DEFAULT_SYS_USER = "sys";

    // Test container defaults
    static final String APP_USER = "test";

    static final String APP_USER_PASSWORD = "test";

    // Restricted user and database names
    private static final List<String> ORACLE_SYSTEM_USERS = Arrays.asList(DEFAULT_SYSTEM_USER, DEFAULT_SYS_USER);

    private String databaseName = DEFAULT_DATABASE_NAME;

    private String username = APP_USER;

    private String password = APP_USER_PASSWORD;

    private boolean usingSid = false;

    /**
     * @deprecated use {@link #OracleContainer(DockerImageName)} instead
     */
    @Deprecated
    public OracleFreeContainer() {
        this(DEFAULT_IMAGE_NAME.withTag(DEFAULT_TAG));
    }

    public OracleFreeContainer(String dockerImageName) {
        this(DockerImageName.parse(dockerImageName));
    }

    public OracleFreeContainer(final DockerImageName dockerImageName) {
        super(dockerImageName);
        dockerImageName.assertCompatibleWith(DEFAULT_IMAGE_NAME);
        preconfigure();
    }

    public OracleFreeContainer(Future<String> dockerImageName) {
        super(dockerImageName);
        preconfigure();
    }

    private void preconfigure() {
        this.waitStrategy =
            new LogMessageWaitStrategy()
                .withRegEx(".*DATABASE IS READY TO USE!.*\\s")
                .withTimes(1)
                .withStartupTimeout(Duration.of(DEFAULT_STARTUP_TIMEOUT_SECONDS, ChronoUnit.SECONDS));

        withConnectTimeoutSeconds(DEFAULT_CONNECT_TIMEOUT_SECONDS);
        addExposedPorts(ORACLE_PORT);
    }

    @Override
    protected void waitUntilContainerStarted() {
        getWaitStrategy().waitUntilReady(this);
    }

    @NotNull
    @Override
    public Set<Integer> getLivenessCheckPortNumbers() {
        return Collections.singleton(getMappedPort(ORACLE_PORT));
    }

    @Override
    public String getDriverClassName() {
        return "oracle.jdbc.driver.OracleDriver";
    }

    @Override
    public String getJdbcUrl() {
        return isUsingSid()
            ? "jdbc:oracle:thin:" + "@" + getHost() + ":" + getOraclePort() + ":" + getSid()
            : "jdbc:oracle:thin:" + "@" + getHost() + ":" + getOraclePort() + "/" + getDatabaseName();
    }

    @Override
    public String getUsername() {
        // An application user is tied to the database, and therefore not authenticated to connect to SID.
        return isUsingSid() ? DEFAULT_SYSTEM_USER : username;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getDatabaseName() {
        return databaseName;
    }

    protected boolean isUsingSid() {
        return usingSid;
    }

    @Override
    public OracleFreeContainer withUsername(String username) {
        if (StringUtils.isEmpty(username)) {
            throw new IllegalArgumentException("Username cannot be null or empty");
        }
        if (ORACLE_SYSTEM_USERS.contains(username.toLowerCase())) {
            throw new IllegalArgumentException("Username cannot be one of " + ORACLE_SYSTEM_USERS);
        }
        this.username = username;
        return self();
    }

    @Override
    public OracleFreeContainer withPassword(String password) {
        if (StringUtils.isEmpty(password)) {
            throw new IllegalArgumentException("Password cannot be null or empty");
        }
        this.password = password;
        return self();
    }

    @Override
    public OracleFreeContainer withDatabaseName(String databaseName) {
        if (StringUtils.isEmpty(databaseName)) {
            throw new IllegalArgumentException("Database name cannot be null or empty");
        }

        if (DEFAULT_DATABASE_NAME.equals(databaseName.toLowerCase())) {
            throw new IllegalArgumentException("Database name cannot be set to " + DEFAULT_DATABASE_NAME);
        }

        this.databaseName = databaseName;
        return self();
    }

    public OracleFreeContainer usingSid() {
        this.usingSid = true;
        return self();
    }

    @Override
    public OracleFreeContainer withUrlParam(String paramName, String paramValue) {
        throw new UnsupportedOperationException("The Oracle Database driver does not support this");
    }

    @SuppressWarnings("SameReturnValue")
    public String getSid() {
        return DEFAULT_SID;
    }

    public Integer getOraclePort() {
        return getMappedPort(ORACLE_PORT);
    }

    @SuppressWarnings("unused")
    public Integer getWebPort() {
        retur null;
    }

    @Override
    public String getTestQueryString() {
        return "SELECT 1 FROM DUAL";
    }

    @Override
    protected void configure() {
        withEnv("ORACLE_PASSWORD", password);

        // Only set ORACLE_DATABASE if different than the default.
        if (databaseName != DEFAULT_DATABASE_NAME) {
            withEnv("ORACLE_DATABASE", databaseName);
        }

        withEnv("APP_USER", username);
        withEnv("APP_USER_PASSWORD", password);
    }
}
