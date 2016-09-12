package org.testcontainers.containers;

/**
 * @author richardnorth
 */
public class PostgreSQLContainer<SELF extends PostgreSQLContainer<SELF>> extends JdbcDatabaseContainer<SELF> {
    public static final String NAME = "postgresql";
    public static final String IMAGE = "postgres";
    public static final Integer POSTGRESQL_PORT = 5432;
    private String databaseName = "test";
    private String username = "test";
    private String password = "test";

    public PostgreSQLContainer() {
        this(IMAGE + ":latest");
    }

    public PostgreSQLContainer(final String dockerImageName) {
        super(dockerImageName);
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

    public SELF withDatabaseName(final String databaseName) {
        this.databaseName = databaseName;
        return self();
    }

    public SELF withUsername(final String username) {
        this.username = username;
        return self();
    }

    public SELF withPassword(final String password) {
        this.password = password;
        return self();
    }
}
