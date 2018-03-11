package org.testcontainers.containers;

import org.testcontainers.utility.TestcontainersConfiguration;

/**
 * @author gusohal
 */
public class OracleContainer extends JdbcDatabaseContainer {

    public static final String NAME = "oracle";
    public static final String IMAGE = TestcontainersConfiguration.getInstance()
            .getProperties().getProperty("oracle.container.image","wnameless/oracle-xe-11g");
    private static final int ORACLE_PORT = 1521;
    private static final int APEX_HTTP_PORT = 8080;

    public OracleContainer() {
        super(IMAGE + "@sha256:15ff9ef50b4f90613c9780b589c57d98a8a9d496decec854316d96396ec5c085");
    }

    public OracleContainer(String dockerImageName) {
        super(dockerImageName);
    }

    @Override
    protected Integer getLivenessCheckPort() {
        return getMappedPort(ORACLE_PORT);
    }

    @Override
    protected void configure() {

        addExposedPorts(ORACLE_PORT, APEX_HTTP_PORT);
    }

    @Override
    public String getDriverClassName() {
        return "oracle.jdbc.OracleDriver";
    }

    @Override
    public String getJdbcUrl() {
        return "jdbc:oracle:thin:" + getUsername() + "/" + getPassword() + "@//" + getContainerIpAddress() + ":" + getOraclePort() + "/" + getSid();
    }

    @Override
    public String getUsername() {
        return "system";
    }

    @Override
    public String getPassword() {
        return "oracle";
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
    protected int getStartupTimeoutSeconds() {
        return 240;
    }
}
