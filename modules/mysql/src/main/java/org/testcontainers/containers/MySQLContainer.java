package org.testcontainers.containers;

/**
 * @author richardnorth
 */
public class MySQLContainer<SELF extends MySQLContainer<SELF>> extends JdbcDatabaseContainer<SELF> {

    public static final String NAME = "mysql";
    public static final String IMAGE = "mysql";
    private static final String MY_CNF_CONFIG_OVERRIDE_PARAM_NAME = "TC_MY_CNF";
    private static final Integer MYSQL_PORT = 3306;

    public MySQLContainer() {
        super(IMAGE + ":latest");
    }

    public MySQLContainer(String dockerImageName) {
        super(dockerImageName);
    }

    @Override
    protected Integer getLivenessCheckPort() {
        return getMappedPort(MYSQL_PORT);
    }

    @Override
    protected void configure() {
        optionallyMapResourceParameterAsVolume(MY_CNF_CONFIG_OVERRIDE_PARAM_NAME, "/etc/mysql/conf.d", "mysql-default-conf");

        addExposedPort(3306);
        addEnv("MYSQL_DATABASE", "test");
        addEnv("MYSQL_USER", "test");
        addEnv("MYSQL_PASSWORD", "test");
        addEnv("MYSQL_ROOT_PASSWORD", "test");
        setCommand("mysqld");
        setStartupAttempts(3);
    }

    @Override
    public String getDriverClassName() {
        return "com.mysql.jdbc.Driver";
    }

    @Override
    public String getJdbcUrl() {
        return "jdbc:mysql://" + getContainerIpAddress() + ":" + getMappedPort(MYSQL_PORT) + "/test";
    }

    @Override
    public String getUsername() {
        return "test";
    }

    @Override
    public String getPassword() {
        return "test";
    }

    @Override
    public String getTestQueryString() {
        return "SELECT 1";
    }

    public SELF withConfigurationOverride(String s) {
        parameters.put(MY_CNF_CONFIG_OVERRIDE_PARAM_NAME, s);
        return self();
    }
}
