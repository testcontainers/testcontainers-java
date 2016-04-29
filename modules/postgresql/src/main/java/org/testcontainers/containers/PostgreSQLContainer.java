package org.testcontainers.containers;

/**
 * @author richardnorth
 */
public class PostgreSQLContainer<SELF extends PostgreSQLContainer<SELF>> extends JdbcDatabaseContainer<SELF> {

    public static final String NAME = "postgresql";
    public static final String IMAGE = "postgres";
    public static final Integer POSTGRESQL_PORT = 5432;

    public PostgreSQLContainer() {
        super(IMAGE + ":latest");
    }

    public PostgreSQLContainer(String dockerImageName) {
        super(dockerImageName);
    }

    @Override
    protected Integer getLivenessCheckPort() {
        return getMappedPort(POSTGRESQL_PORT);
    }

    @Override
    protected void configure() {

        addExposedPort(POSTGRESQL_PORT);
        addEnv("POSTGRES_DATABASE", "test");
        addEnv("POSTGRES_USER", "test");
        addEnv("POSTGRES_PASSWORD", "test");
        setCommand("postgres");
    }

    @Override
    public String getDriverClassName() {
        return "org.postgresql.Driver";
    }

    @Override
    public String getJdbcUrl() {
        return "jdbc:postgresql://" + getContainerIpAddress() + ":" + getMappedPort(POSTGRESQL_PORT) + "/test";
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
}
