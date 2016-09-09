package org.testcontainers.containers;

/**
 * @author richardnorth
 */
public class PostgreSQLContainer<SELF extends PostgreSQLContainer<SELF>> extends JdbcDatabaseContainer<SELF> {
    public static final String NAME = "postgresql";
    public static final String IMAGE = "postgres";
    public static final Integer POSTGRESQL_PORT = 5432;
    final String databaseName;
    final String username;
    final String password;

    public PostgreSQLContainer() {
        this(IMAGE + ":latest");
    }

    public PostgreSQLContainer(final String dockerImageName) {
        this(dockerImageName, "test", "test", "test");
    }

    public PostgreSQLContainer(
        final String dockerImageName,
        final String databaseName,
        final String username,
        final String password
    ) {
        super(dockerImageName);
        this.databaseName = databaseName;
        this.username = username;
        this.password = password;
    }

    @Override
    protected Integer getLivenessCheckPort() {
        return getMappedPort(POSTGRESQL_PORT);
    }

    @Override
    protected void configure() {

        addExposedPort(POSTGRESQL_PORT);
        addEnv("POSTGRES_DB", databaseName);
        addEnv("POSTGRES_USER", username);
        addEnv("POSTGRES_PASSWORD", password);
        setCommand("postgres");
    }

    @Override
    public String getDriverClassName() {
        return "org.postgresql.Driver";
    }

    @Override
    public String getJdbcUrl() {
        return "jdbc:postgresql://" + getContainerIpAddress() + ":" + getMappedPort(POSTGRESQL_PORT) + "/" + databaseName;
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
