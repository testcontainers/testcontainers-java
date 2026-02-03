package org.testcontainers.oracle;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Testcontainers implementation for Oracle Database Free.
 * <p>
 * Supported image: {@code gvenzl/oracle-free}
 * <p>
 * Exposed ports: 1521
 */
public class OracleContainer extends JdbcDatabaseContainer<OracleContainer> {

    static final String NAME = "oracle";

    private static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse("gvenzl/oracle-free");

    static final String DEFAULT_TAG = "slim";

    static final String IMAGE = DEFAULT_IMAGE_NAME.getUnversionedPart();

    static final int ORACLE_PORT = 1521;

    private static final int DEFAULT_STARTUP_TIMEOUT_SECONDS = 60;

    private static final int DEFAULT_CONNECT_TIMEOUT_SECONDS = 60;

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

    /**
     * Password for Oracle system user (e.g. SYSTEM/SYS). Defaults to {@link #APP_USER_PASSWORD}
     * for backwards compatibility, but can be customized independently via {@link #withSystemPassword(String)}.
     */
    private String oraclePassword = APP_USER_PASSWORD;

    /**
     * Tracks whether {@link #withSystemPassword(String)} was called to avoid overriding
     * the system password when {@link #withPassword(String)} is used for the application user only.
     */
    private boolean systemPasswordExplicitlySet = false;

    private boolean usingSid = false;

    public OracleContainer(String dockerImageName) {
        this(DockerImageName.parse(dockerImageName));
    }

    public OracleContainer(final DockerImageName dockerImageName) {
        super(dockerImageName);
        dockerImageName.assertCompatibleWith(DEFAULT_IMAGE_NAME);
        waitingFor(
            Wait
                .forLogMessage(".*DATABASE IS READY TO USE!.*\\s", 1)
                .withStartupTimeout(Duration.ofSeconds(DEFAULT_STARTUP_TIMEOUT_SECONDS))
        );
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
        try {
            Class.forName("oracle.jdbc.OracleDriver");
            return "oracle.jdbc.OracleDriver";
        } catch (ClassNotFoundException e) {
            return "oracle.jdbc.driver.OracleDriver";
        }
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
        // When connecting via SID we authenticate as SYSTEM. Use the dedicated system password.
        return isUsingSid() ? oraclePassword : password;
    }

    @Override
    public String getDatabaseName() {
        return databaseName;
    }

    protected boolean isUsingSid() {
        return usingSid;
    }

    @Override
    public OracleContainer withUsername(String username) {
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
    public OracleContainer withPassword(String password) {
        if (StringUtils.isEmpty(password)) {
            throw new IllegalArgumentException("Password cannot be null or empty");
        }
        this.password = password;
        // Maintain backwards compatibility: if system password wasn't set explicitly,
        // align it with the application user's password.
        if (!systemPasswordExplicitlySet) {
            this.oraclePassword = password;
        }
        return self();
    }

    /**
     * Sets the password for the Oracle system user (SYSTEM/SYS). This is independent from the
     * application user password set via {@link #withPassword(String)}.
     *
     * @param oraclePassword password for SYSTEM/SYS users inside the container
     * @return this container instance
     */
    public OracleContainer withSystemPassword(String oraclePassword) {
        if (StringUtils.isEmpty(oraclePassword)) {
            throw new IllegalArgumentException("Oracle password cannot be null or empty");
        }
        this.oraclePassword = oraclePassword;
        this.systemPasswordExplicitlySet = true;
        return self();
    }

    @Override
    public OracleContainer withDatabaseName(String databaseName) {
        if (StringUtils.isEmpty(databaseName)) {
            throw new IllegalArgumentException("Database name cannot be null or empty");
        }

        if (DEFAULT_DATABASE_NAME.equals(databaseName.toLowerCase())) {
            throw new IllegalArgumentException("Database name cannot be set to " + DEFAULT_DATABASE_NAME);
        }

        this.databaseName = databaseName;
        return self();
    }

    public OracleContainer usingSid() {
        this.usingSid = true;
        return self();
    }

    @Override
    public OracleContainer withUrlParam(String paramName, String paramValue) {
        throw new UnsupportedOperationException("The Oracle Database driver does not support this");
    }

    @SuppressWarnings("SameReturnValue")
    public String getSid() {
        return DEFAULT_SID;
    }

    public Integer getOraclePort() {
        return getMappedPort(ORACLE_PORT);
    }

    @Override
    public String getTestQueryString() {
        return "SELECT 1 FROM DUAL";
    }

    @Override
    protected void configure() {
        // Configure system user password independently from application user's password
        withEnv("ORACLE_PASSWORD", oraclePassword);

        // Only set ORACLE_DATABASE if different than the default.
        if (databaseName != DEFAULT_DATABASE_NAME) {
            withEnv("ORACLE_DATABASE", databaseName);
        }

        withEnv("APP_USER", username);
        withEnv("APP_USER_PASSWORD", password);
    }
}
