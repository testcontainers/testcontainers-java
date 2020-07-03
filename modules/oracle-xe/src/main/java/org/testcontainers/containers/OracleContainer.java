package org.testcontainers.containers;

import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.TestcontainersConfiguration;

import java.util.concurrent.Future;

/**
 * @author gusohal
 */
public class OracleContainer extends JdbcDatabaseContainer<OracleContainer> {

    public static final String NAME = "oracle";

    private static final int ORACLE_PORT = 1521;
    private static final int APEX_HTTP_PORT = 8080;

    private static final int DEFAULT_STARTUP_TIMEOUT_SECONDS = 240;
    private static final int DEFAULT_CONNECT_TIMEOUT_SECONDS = 120;

    private String username = "system";
    private String password = "oracle";

    private static String resolveImageName() {
        String image = TestcontainersConfiguration.getInstance()
            .getProperties().getProperty("oracle.container.image");

        if (image == null) {
            throw new IllegalStateException("An image to use for Oracle containers must be configured. " +
                "To do this, please place a file on the classpath named `testcontainers.properties`, " +
                "containing `oracle.container.image=IMAGE`, where IMAGE is a suitable image name and tag.");
        }
        return image;
    }

    /**
     * @deprecated use {@link OracleContainer(DockerImageName)} instead
     */
    @Deprecated
    public OracleContainer() {
        this(resolveImageName());
    }

    /**
     * @deprecated use {@link OracleContainer(DockerImageName)} instead
     */
    @Deprecated
    public OracleContainer(String dockerImageName) {
        this(DockerImageName.parse(dockerImageName));
    }

    public OracleContainer(final DockerImageName dockerImageName) {
        super(dockerImageName);
        preconfigure();
    }

    public OracleContainer(Future<String> dockerImageName) {
        super(dockerImageName);
        preconfigure();
    }

    private void preconfigure() {
        withStartupTimeoutSeconds(DEFAULT_STARTUP_TIMEOUT_SECONDS);
        withConnectTimeoutSeconds(DEFAULT_CONNECT_TIMEOUT_SECONDS);
        addExposedPorts(ORACLE_PORT, APEX_HTTP_PORT);
    }

    @Override
    protected Integer getLivenessCheckPort() {
        return getMappedPort(ORACLE_PORT);
    }

    @Override
    public String getDriverClassName() {
        return "oracle.jdbc.OracleDriver";
    }

    @Override
    public String getJdbcUrl() {
        return "jdbc:oracle:thin:" + getUsername() + "/" + getPassword() + "@" + getHost() + ":" + getOraclePort() + ":" + getSid();
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
    public OracleContainer withUsername(String username) {
        this.username = username;
        return self();
    }

    @Override
    public OracleContainer withPassword(String password) {
        this.password = password;
        return self();
    }

    @Override
    public OracleContainer withUrlParam(String paramName, String paramValue) {
        throw new UnsupportedOperationException("The OracleDb does not support this");
    }

    @SuppressWarnings("SameReturnValue")
    public String getSid() {
        return "xe";
    }

    public Integer getOraclePort() {
        return getMappedPort(ORACLE_PORT);
    }

    @SuppressWarnings("unused")
    public Integer getWebPort() {
        return getMappedPort(APEX_HTTP_PORT);
    }

    @Override
    public String getTestQueryString() {
        return "SELECT 1 FROM DUAL";
    }
}
