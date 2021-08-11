package org.testcontainers.containers;

import com.google.common.collect.Sets;
import org.apache.commons.lang.StringUtils;
import org.testcontainers.utility.DockerImageName;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;

public class OracleContainer<SELF extends OracleContainer<SELF>> extends JdbcDatabaseContainer<SELF> {

    public static final String NAME = "oracle";
    private static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse("gvenzl/oracle-xe");

    @Deprecated
    public static final String DEFAULT_TAG = "18.4.0-slim";
    @Deprecated
    public static final String IMAGE = DEFAULT_IMAGE_NAME.getUnversionedPart();

    private static final int ORACLE_PORT = 1521;
    private static final int APEX_HTTP_PORT = 8080;

    private static final int DEFAULT_STARTUP_TIMEOUT_SECONDS = 240;
    private static final int DEFAULT_CONNECT_TIMEOUT_SECONDS = 120;
    private static final List<String> ORACLE_SYSTEM_USERS = Arrays.asList("system", "sys");

    private String username = "test";
    private String password = "test";

    /**
     * @deprecated use {@link OracleContainer(DockerImageName)} instead
     */
    @Deprecated
    public OracleContainer() {
        this(DEFAULT_IMAGE_NAME.withTag(DEFAULT_TAG));
    }

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
    public Set<Integer> getLivenessCheckPortNumbers() {
        return Sets.newHashSet(ORACLE_PORT);
    }

    @Override
    public String getDriverClassName() {
        return "oracle.jdbc.OracleDriver";
    }

    @Override
    public String getJdbcUrl() {
        return "jdbc:oracle:thin:" + getUsername() + "/" + getPassword() + "@" + getHost() + ":" + getOraclePort() + "/xepdb1";
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
    public SELF withUsername(String username) {
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
    public SELF withPassword(String password) {
        if (StringUtils.isEmpty(password)) {
            throw new IllegalArgumentException("Password cannot be null or empty");
        }
        this.password = password;
        return self();
    }

    @Override
    public SELF withUrlParam(String paramName, String paramValue) {
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

    @Override
    protected void configure() {
        addEnv("ORACLE_PASSWORD", password);
        addEnv("APP_USER", username);
        addEnv("APP_USER_PASSWORD", password);
    }
}
