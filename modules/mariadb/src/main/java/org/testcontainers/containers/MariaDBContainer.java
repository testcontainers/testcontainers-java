package org.testcontainers.containers;

/**
 * Container implementation for the MariaDB project.
 *
 * @author Miguel Gonzalez Sanchez
 */
public class MariaDBContainer<SELF extends MariaDBContainer<SELF>> extends JdbcDatabaseContainer<SELF> {

    public static final String NAME = "mariadb";
    public static final String IMAGE = "mariadb";
    public static final String DEFAULT_TAG = "10.3.6";

    private static final Integer MARIADB_PORT = 3306;
    private static final String MARIADB_USER = "test";
    private static final String MARIADB_PASSWORD = "test";
    private static final String MARIADB_DATABASE = "test";
    private static final String MY_CNF_CONFIG_OVERRIDE_PARAM_NAME = "TC_MY_CNF";

    public MariaDBContainer() {
        super(IMAGE + ":" + DEFAULT_TAG);
    }

    public MariaDBContainer(String dockerImageName) {
        super(dockerImageName);
    }

    @Override
    protected Integer getLivenessCheckPort() {
        return getMappedPort(MARIADB_PORT);
    }

    @Override
    protected void configure() {
        optionallyMapResourceParameterAsVolume(MY_CNF_CONFIG_OVERRIDE_PARAM_NAME, "/etc/mysql/conf.d", "mariadb-default-conf");

        addExposedPort(MARIADB_PORT);
        addEnv("MYSQL_DATABASE", MARIADB_DATABASE);
        addEnv("MYSQL_USER", MARIADB_USER);
        addEnv("MYSQL_PASSWORD", MARIADB_PASSWORD);
        addEnv("MYSQL_ROOT_PASSWORD", MARIADB_PASSWORD);
        setCommand("mysqld");
        setStartupAttempts(3);
    }

    @Override
    public String getDriverClassName() {
        return "org.mariadb.jdbc.Driver";
    }

    @Override
    public String getJdbcUrl() {
        return "jdbc:mariadb://" + getContainerIpAddress() + ":" + getMappedPort(MARIADB_PORT) + "/test";
    }

    @Override
    public String getDatabaseName() {
    	return MARIADB_DATABASE;
    }

    @Override
    public String getUsername() {
        return MARIADB_USER;
    }

    @Override
    public String getPassword() {
        return MARIADB_PASSWORD;
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
