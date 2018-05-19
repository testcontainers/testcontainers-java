package org.testcontainers.containers;

import org.testcontainers.utility.LicenseAcceptance;

/**
 * @author Stefan Hufschmidt
 */
public class MSSQLServerContainer<SELF extends MSSQLServerContainer<SELF>> extends JdbcDatabaseContainer<SELF> {
    public static final String NAME = "mssqlserver";
    public static final String IMAGE = "microsoft/mssql-server-linux";
    public static final String DEFAULT_TAG = "2017-CU6";

    public static final Integer MS_SQL_SERVER_PORT = 1433;
    private String username = "SA";
    private String password = "A_Str0ng_Required_Password";

    public MSSQLServerContainer() {
        this(IMAGE + ":" + DEFAULT_TAG);
    }

    public MSSQLServerContainer(final String dockerImageName) {
        super(dockerImageName);
    }

    @Override
    protected Integer getLivenessCheckPort() {
        return getMappedPort(MS_SQL_SERVER_PORT);
    }

    @Override
    protected void configure() {
        addExposedPort(MS_SQL_SERVER_PORT);

        LicenseAcceptance.assertLicenseAccepted(this.getDockerImageName());
        addEnv("ACCEPT_EULA", "Y");

        addEnv("SA_PASSWORD", password);
    }

    @Override
    public String getDriverClassName() {
        return "com.microsoft.sqlserver.jdbc.SQLServerDriver";
    }

    @Override
    public String getJdbcUrl() {
        return "jdbc:sqlserver://" + getContainerIpAddress() + ":" + getMappedPort(MS_SQL_SERVER_PORT);
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
}
